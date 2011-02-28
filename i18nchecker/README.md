I18N Checker
============

I18N Checker is an internationalization and localization tool for Java.

It can be used for verifying strings in Java sources and resource bundles.
Also serves for exporting all strings for translation and importing them back to resource bundles.
Current implementation works with well-known structure of directories (as we have developed
it for our own repository which is organized this way). 
The tool startup parameter is "top level dirs" list. Each top level directory
should contain modules and each module is recognized using "src" subdirectory.
In this example the top level directories are "modules_group1,module_group2":
+---modules_group1
|   +---module1
|   |   +---src
|   |   |   \---com...
|   \---module2
|       \---src
|           \---com...
+---modules_group2
|   +---moduleAAA
|   |   +---src
|   |   |   \---com...
|   \---moduleBBB
|       \---src
|           \---com...
...

Usages
------

Main class is org.i18nchecker.I18nChecker. It has several setters and some executable methods.

Currently I18N Checker works in a few different modes, depending on what properties are set
(or what method is called). TopDirs and RootDir properties are mandatory.

* Mode 1 - Print errors: prints all errors in I18N (no other extra property is required)

* Mode 2 - Export to CSV for translation (language and exportToFile property must be set)

* Mode 3 - Apply translated CSV into translated resource bundle files (language and importFromFile property must be set)

* Mode 4 - Run as unit test verifying that there are no regressions in I18N. runAsTest method should be called and it takes all required parameters.

* TODO: Mode 5 - like Mode 1, but from command line (i18nchecker.jar should be executable)

Example
-------
See this project build script. It uses playground directory as example of incorrect sources/resources bundles.
Run i18n-consistency-check target from build.xml.

Also see this project's unit test - it shows how I18N Checker can run as unit test (to prevent i18n regressions).
Test should pass as some i18n errors are expected. Try to add one more i18n error to sources in playground
and run the test again. It should fail.
