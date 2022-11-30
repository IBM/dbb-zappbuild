# File Property Management

In a ZOS application environment, we normally see scenarios like application files/programs of same type being build with different properties. 

For example, we can see COBOL object modules that are link edited with option NCAL and without option NCAL under the same application for different purpose. This used to be customized in legacy SCMs using process type/name used to build the file. This documents talks about how to customize the file level build properties in zAppBuild framework. 

In order to handle such scenarios to override default file properties for specific set of file/files, zAppBuild has got two options:
  1. Individual file property -  Override for individual files
  2. Language Definition property - Override for group of files


## 1. Individual File Property




## 2. Language File Definition