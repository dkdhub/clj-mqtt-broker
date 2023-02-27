{:dev      {:global-vars   {*warn-on-reflection* true}

            :target-path   "target/%s"
            :clean-targets ^{:protect false} [:target-path]

            :dependencies [[ch.qos.logback/logback-classic "1.4.5"
                            :exclusions [org.slf4j/slf4j-api]]
                           [org.slf4j/jul-to-slf4j "2.0.6"]
                           [org.slf4j/jcl-over-slf4j "2.0.6"]
                           [org.slf4j/log4j-over-slf4j "2.0.6"]
                           [org.clojure/tools.logging "1.2.4"]]}

 :provided {:dependencies      [[org.clojure/clojure "1.11.1"]
                                [org.slf4j/log4j-over-slf4j "2.0.6"]]
            :source-paths      #{"src-clj"}
            :java-source-paths #{"src-java"}
            :resource-paths    ["resources"]

            :javac-options     ["-source" "9" "-target" "9" "-g:none"]

            :jar-exclusions    [#"\.java", #"^moquette.conf$"]}

 :jar      {:aot :all}}
