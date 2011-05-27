(defproject xbxnotify "1.0.0-SNAPSHOT"
  :description "Notifies an e-mail address (for example, a SMS number's e-mail address) when friends sign on to Xbox Live."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [net.sourceforge.htmlunit/htmlunit "2.8"]
                 [org.apache.commons/commons-email "1.2"]
                 [enlive "1.0.0"]
                 [clargon "1.0.0"]]
  :dev-dependencies [[swank-clojure "1.2.0"]]
  :main xbxnotify
  :repositories {"sonatype" "https://oss.sonatype.org/content/repositories/releases/"})
