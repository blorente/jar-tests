# jar-tests
name says it all, really

A tool to extract information about dependencies. It can read single classpath files from a path, or a list of them from stdin. Therefore, there are two ways to run it.

```bash
# Absolute path of one file
java -jar util.jar /home/borja/my/project/classpath.dat
# Multiple files from stdin
find /home/borja/this/has/to/be/absolute -name "classpath.dat" -maxdepth 3 | java -jar util.jar
```
