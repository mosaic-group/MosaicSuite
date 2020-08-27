
.. _mosaicsuite-development:

=======================
Development
=======================

Source code
===========

MosaicSuite code can be found on public `MOSAIC git server <https://git.mpi-cbg.de/mosaic/MosaicSuite/tree/master>`__.
Code can be downloaded by following git command:

.. code:: bash

    git clone https://git.mpi-cbg.de/mosaic/MosaicSuite.git


If you'd like to contribute bug fixes or new functions to any of the plugins, or are interested in using the source code in your own projects, please make sure to first download the latest version. The code is constantly evolving. If you think your additions could be useful also for other users, please send them to us and we will include them in future releases. Your contributions are highly appreciated!


How to build
============

To build MosaicSuite package you need Java in version at least 1.8 and maven.
To start build run in root directory of your cloned MosaicSuite following command:

.. code:: bash

    mvn package -Dmaven.test.skip=true

After successful build in ``target`` directory there will be two jars:

- MosaicSuite-1.0.19.jar
- MosaicSuite-1.0.19_Full.jar

The second one is a MosaicSuite bundled with all needed dependencies. Version of MosaicSuite is set
accoriding to number in main ``pom.xml`` of a project.


How to install
==============

To use MosaicSuite in Fiji/Imagej application it is enough to copy built ``MosaicSuite<version>_Full.jar`` to ``plugins``
directory in root installation directory of your Fiji/ImageJ.

Sometimes in development it would be convenient to copy output jar file automatically. It might be achieved by
setting environment variable with path to ``plugins`` directory of your Fiji/ImageJ application:

.. code:: bash

    export FIJI_MOSAIC_PLUGIN_PATH=/full/path/to/your/Fiji.app/plugins/Mosaic_ToolSuite

and then uncommenting following part of main ``pom.xml``:

.. code:: java

    <!--            <plugin>
                    <artifactId>maven-resources-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>copy-resources</id>
                            <phase>package</phase>
                            <goals>
                                <goal>copy-resources</goal>
                            </goals>
                            <configuration>
                                <outputDirectory>${env.FIJI_MOSAIC_PLUGIN_PATH}</outputDirectory>
                                <resources>
                                    <resource>
                                        <directory>target</directory>
                                        <includes>
                                            <include>${project.artifactId}-${project.version}_Full.jar</include>
                                        </includes>
                                        <filtering>false</filtering>
                                    </resource>
                                </resources>
                            </configuration>
                        </execution>
                    </executions>
                </plugin> -->

With these modifications every time when ``mvn package`` command is executed, output jar is automatically copied to Fiji/Imagej app.


How to test
===========

One of the best way to develop and test code is to use IDE (like Eclipse, IntelliJ,...). Please refer to documentation
of IDE of your choice to find out how to import maven project and run tests.
Nevertheless very often one might need to run test(s) using command line only and for that information please refer to next chapters of documentation.

Running all tests
-----------------

To run all tests in MosaicSuite run:

.. code:: bash

    mvn test

.. note ::
   | Some of the test require special infrastructure (tests that verify "run on cluster" functionality).
   | If you do not have such access run all tests and ignore errors coming from test*Cluster, SshTest tests.

Running single test suite
-------------------------

Running single test suite:

.. code:: bash

    mvn test -Dtest=MatrixTest


Running single test within test suite
-------------------------------------

Running single test within a given test suite:

.. code:: bash

    mvn test -Dtest=CSVTest#testWriteAppend