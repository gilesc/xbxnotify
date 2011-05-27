(ns xbxnotify
  (:gen-class)
  (:require   
   [net.cgrand.enlive-html :as html]
   [clojure.xml :as xml]
   [clojure.contrib.string :as string])
  (:import
   [com.gargoylesoftware.htmlunit WebClient]
   [org.apache.commons.mail SimpleEmail])
  (:use
   clargon.core
   [clojure.set :only [difference]]))

(def *client* (WebClient.))

(def friends-url "http://live.xbox.com/en-US/FriendCenter")

(defn login [email passwd]
  (let [page (. *client* (getPage friends-url))
       form (first (.getForms page))
       setattr (fn [attr val]
                 (.setValueAttribute
                  (.getInputByName form attr) val))]
   (setattr "login" email)
   (setattr "passwd" passwd)
   (let [page (. (.getInputByName form "SI") click)]
     (Thread/sleep 5000)
     page)))

(defn parse-status [friend]
  (zipmap [:tag :status]
          (map #(.trim (first (html/select friend [% html/text])))
               [:.fc-gtag-link :.fc-presence-text])))

(defn parse-friends [page]
  (let [nodes (xml/parse
               (java.io.ByteArrayInputStream.
                (.getBytes
                 (string/replace-re #"(?s)<script(.+?)</script>" "" (.asXml page)))))]
    (zipmap [:online :offline]
            (map #(map parse-status (html/select nodes [% :.fc-friend]))
                 [:#fc-online :#fc-offline]))))

;;;props to: http://will.groppe.us/post/406065542/sending-email-from-clojure
(defn send-gmail [sms gmail passwd msg]
  (doto (SimpleEmail.)
    (.setHostName "smtp.gmail.com")
    (.setSslSmtpPort "465")
    (.setSSL true)
    (.addTo sms)
    (.setFrom gmail "(xbxfriends auto-notify)")
    (.setSubject msg)
    (.setMsg msg)
    (.setAuthentication gmail passwd)
    (.send)))


(defn -main [& args]
  (let [params (clargon args
                        (required ["-u" "--xbox-email"])
                        (required ["-p" "--xbox-password"])
                        (required ["-s" "--sms-address"])
                        (required ["-g" "--gmail-address"])
                        (required ["-gp" "--gmail-password"]))
        page (login (params :xbox-email) (params :xbox-password))]
    (if-not (contains? #{0 3}
                       (count
                        (filter identity
                                (map params [:sms-address :gmail-address :gmail-password]))))
      (do
        (prn "Need all three of: sms-address, gmail-address, and gmail-password to do SMS notifications.")
        (throw (Exception.))))
    (loop [friends {:online []}]
      (let [new-friends (parse-friends (.refresh page))
            [old-tags new-tags] (map #(set (map :tag (:online %)))
                                     [friends new-friends])
            logged-on (difference new-tags old-tags)
            logged-off (difference old-tags new-tags)
            msg (case (count logged-on)
                      0 nil
                      1 (str (first logged-on " has logged on."))
                      (str (string/join " and " logged-on)
                           " have logged on"))]
        (if msg
          (do
            (prn msg)
            (if (:sms-address params)
              (apply send-gmail
                     (concat
                      (map params [:sms-address :gmail-address :gmail-password])
                      [msg])))))
        (Thread/sleep (* 1000 500))
        (recur new-friends)))))
