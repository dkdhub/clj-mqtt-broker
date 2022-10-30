{:dev      {:global-vars   {*warn-on-reflection* true}

            :target-path   "target/%s"
            :clean-targets ^{:protect false} [:target-path]

            :dependencies [[ch.qos.logback/logback-classic "1.2.11"
                            :exclusions [org.slf4j/slf4j-api]]
                           [org.slf4j/jul-to-slf4j "1.7.36"]
                           [org.slf4j/jcl-over-slf4j "1.7.36"]
                           [org.slf4j/log4j-over-slf4j "1.7.36"]
                           [org.clojure/tools.logging "1.2.4"]]

            :plugins       []}

 :provided {:dependencies      [[org.clojure/clojure "1.11.1"]]
            :source-paths      #{"src-clj"}
            :java-source-paths #{"src-java"}
            :resource-paths    ["resources"]

            :javac-options     ["-source" "9" "-target" "9" "-g:none"]

            :jar-exclusions    [#"\.java"]}

 :jar      {:aot :all}}
