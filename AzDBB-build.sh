#! /bin/sh 
## Az/DBB Demo- DBB Build v1.2 (njl) 
. $HOME/.profile 
MyWorkDir=$1 ; cd $MyWorkDir 
MyWorkSpace=$2 
MyApp=$3 
BuildMode="$4 $5" #DBB Build modes:  --impactBuild,  --reset, --fullBuild, '--fullbuild --scanOnly' 
zAppBuild=$HOME/dbb-zappbuild/build.groovy 
echo "**************************************************************"  
echo "**     Started:  DBB Build on HOST/USER: $(uname -Ia)/$USER" 
echo "**                        MyWorkDir:" $PWD 
echo "**                      MyWorkspace:" $MyWorkSpace    
echo "**                            MyApp:" $MyApp 
echo "**    	           DBB Build Mode:" $BuildMode 
echo "**                   zAppBuild Path:" $zAppBuild 
echo "**                         DBB_HOME:" $DBB_HOME  
echo "** " 
echo " ** Git Status  for MyWorkSpace:"   
git -C $MyWorkSpace status   
groovyz $zAppBuild  --workspace $MyWorkSpace --application $MyApp  -outDir . --hlq $USER  --logEncoding UTF-8  $BuildMode 
if [ "$?" -ne "0" ]; then 
  echo "DBB Build Error. Check the build log for details" 
  exit 12 
fi 

## Except for the reset mode, check for "nothing to build" condition and throw an error to stop pipeline 
if [ "$BuildMode" != "--reset" ]; then  
buildlistsize=$(wc -c < $(find . -name buildList.txt))  
if [ $buildlistsize = 0 ]; then  
  echo "*** Build Error:  No source changes detected.   RC=12" 
  exit 12     
fi 
else 
  echo "*** DBB Reset completed" 
fi 

exit 0 