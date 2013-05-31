#!/bin/bash

# error out if any statements fail
set -e
#set -x

function usage() {
  echo "$0 [options] <tag>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo
  echo "Options:"
  echo " -h          : Print usage"
  echo " -b <branch> : Branch to release from (eg: trunk, 2.1.x, ...)"
  echo " -r <rev>    : Revision to release (eg: 12345)"
  echo " -g <ver>    : GeoTools version/revision (eg: 2.7.4, trunk:12345)"
  echo " -w <ver>    : GeoWebCache version/revision (eg: 1.3-RC1, stable:abcd)"
  echo " -u <user>   : git username"
  echo " -e <passwd> : git email"
  echo
  echo "Environment variables:"
  echo " SKIP_BUILD : Skips main release build"
  echo " SKIP_INSTALLERS : Skips building of mac and windows installers"
  echo " SKIP_GT : Skips the GeoTools build"
  echo " SKIP_GWC : Skips the GeoWebCache build"
}

# parse options
while getopts "hb:r:g:w:u:e:" opt; do
  case $opt in
    h)
      usage
      exit
      ;;
    b)
      branch=$OPTARG
      ;;
    r)
      rev=$OPTARG
      ;;
    g)
      gt_ver=$OPTARG
      ;;
    w)
      gwc_ver=$OPTARG
      ;;
    u)
      git_user=$OPTARG
      ;;
    e)
      git_email=$OPTARG
      ;;
    \?)
      usage
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument"
      exit 1
      ;;
  esac 
done

# clear options to parse main arguments
shift $(( OPTIND -1 ))
tag=$1

# sanity check
if [ -z $tag ] || [ ! -z $2 ]; then
  usage
  exit 1
fi

# load properties + functions
. "$( cd "$( dirname "$0" )" && pwd )"/properties
. "$( cd "$( dirname "$0" )" && pwd )"/functions

# more sanity checks
if [ `is_version_num $tag` == "0" ]; then
  echo "$tag is a not a valid release tag"
  exit 1
fi
if [ `is_primary_branch_num $tag` == "1" ]; then
  echo "$tag is a not a valid release tag, can't be same as primary branch name"
  exit 1
fi

echo "Building release with following parameters:"
echo "  branch = $branch"
echo "  revision = $rev"
echo "  tag = $tag"
echo "  geotools = $gt_ver"
echo "  geowebcache = $gwc_ver"

echo "maven/java settings:"
mvn -version

if [ ! -z $git_user ] && [ ! -z $git_email ]; then
  git_opts="--author '$git_user <$git_email>'"
fi

# move to root of source tree
pushd .. > /dev/null

# clear out any changes
git reset --hard HEAD

# checkout and update primary / release branches
git checkout rel_$branch
git pull origin rel_$branch
git checkout $branch
git pull origin $branch

# check to see if a release branch already exists
set +e && git checkout rel_$tag && set -e
if [ $? == 0 ]; then
  # release branch already exists, kill it
  git checkout $branch
  echo "branch rel_$tag exists, deleting it"
  git branch -D rel_$tag
fi

# checkout the branch to release from
git checkout $branch

# create a release branch
git checkout -b rel_$tag $rev

# generate release notes
jira_id=`get_jira_id $tag`
if [ -z $jira_id ]; then
  echo "Could not locate release $tag in JIRA"
  exit -1
fi
echo "jira id = $jira_id"

# update README
#search?jql=project+%3D+%22GEOS%22+and+fixVersion+%3D+%222.2-beta2%22"

# setup geotools and geowebcache dependencies
if [ -z $SKIP_GT ]; then
  if [ ! -z $gt_ver ]; then
    # deterine if this is a revision or version number 
    if [ "`is_version_num $gt_ver`" == "1" ]; then
       gt_tag=$gt_ver
    fi

    # if version specified as branch/revision check it out
    if [ -z $gt_tag ]; then
      arr=(${gt_ver//@/ }) 
      if [ "${#arr[@]}" != "2" ]; then
         echo "Bad version $gt_ver, must be specified as <branch>@<revision>"
         exit 1
      fi

      gt_branch=${arr[0]}
      gt_rev=${arr[1]}
      gt_dir=geotools
      if [ ! -e $gt_dir ]; then
         echo "cloning geotools repo from $GT_GIT_URL"
         git clone $GT_GIT_URL $gt_dir
      fi

      # checkout branch/rev
      pushd $gt_dir > /dev/null
      echo "checking out geotools ${gt_branch}@${gt_rev}"
      git checkout $gt_rev

      # build geotools
      mvn clean install -DskipTests -Dall
      popd > /dev/null

      # derive the gt version
      gt_tag=`get_pom_version $git_dir/pom.xml`
    fi
  fi
fi

if [ ! -z $gt_tag ]; then
  echo "GeoTools version = $gt_tag"
fi

# setup geowebcache dependency
if [ -z $SKIP_GWC ]; then
  if [ ! -z $gwc_ver ]; then
    # deterine if this is a revision or version number 
    if [ "`is_version_num $gwc_ver`" == "1" ]; then
       gwc_tag=$gwc_ver
    fi

    # if version specified as branch/revision check it out
    if [ -z $gwc_tag ]; then
      arr=(${gwc_ver//@/ }) 
      if [ "${#arr[@]}" != "2" ]; then
         echo "Bad version $gwc_ver, must be specified as <branch>:<revision>"
         exit 1 
      fi

      gwc_branch=${arr[0]}
      gwc_rev=${arr[1]}
      gwc_dir=geowebcache/$gwc_branch/$gwc_rev
      if [ ! -e $gwc_dir ]; then
         mkdir -p $gwc_dir 
         echo "checking out geowebache ${gwc_branch}@${gwc_rev}"
         git clone $GWC_GIT_URL $gwc_dir
         pushd $gwc_dir > /dev/null
         git checkout $gwc_rev
         popd > /dev/null
      fi

      # build geowebache
      pushd $gwc_dir/geowebcache > /dev/null
      mvn -o clean install -DskipTests

      # derive the gwc version
      gwc_tag=`get_pom_version pom.xml`

      popd > /dev/null
    fi
  fi
fi

if [ ! -z $gwc_tag ]; then
  echo "GeoWebCache version = $gwc_tag"
fi

# update geotools + geowebcache versions
if [ ! -z $gt_tag ]; then
  sed -i "s/\(<gt.version>\).*\(<\/gt.version>\)/\1$gt_tag\2/g" src/pom.xml
else
  # look up from pom instead
  gt_tag=`cat src/pom.xml | grep "<gt.version>" | sed 's/ *<gt.version>\(.*\)<\/gt.version>/\1/g'`
fi

if [ ! -z $gwc_tag ]; then
  sed -i "s/\(<gwc.version>\).*\(<\/gwc.version>\)/\1$gwc_tag\2/g" src/pom.xml
else
  gwc_tag=`cat src/pom.xml | grep "<gwc.version>" | sed 's/ *<gwc.version>\(.*\)<\/gwc.version>/\1/g'`
fi

# update the release notes
notes=RELEASE_NOTES.txt
pushd src/release > /dev/null
sed -i "s/@VER@/$tag/g" $notes
sed -i "s/@DATE@/`date "+%b %d, %Y"`/g" $notes
sed -i "s/@JIRA_VER@/$jira_id/g" $notes

gt_ver_info=$gt_tag
if [ ! -z $gt_rev ]; then
  gt_ver_info="$gt_ver_info, rev $gt_rev"
fi

gwc_ver_info=$gwc_tag
if [ ! -z $gwc_rev ]; then
  gwc_ver_info="$gwc_ver_info, rev $gwc_rev"
fi

sed -i "s/@GT_VER@/$gt_ver_info/g" $notes
sed -i "s/@GWC_VER@/$gwc_ver_info/g" $notes

popd > /dev/null

# update version numbers
old_ver=`get_pom_version src/pom.xml`

echo "updating version numbers from $old_ver to $tag"
find src -name pom.xml -exec sed -i "s/$old_ver/$tag/g" {} \;
find doc -name conf.py -exec sed -i "s/$old_ver/$tag/g" {} \;

pushd src/release > /dev/null
sed -i "s/$old_ver/$tag/g" *.xml installer/win/*.nsi installer/win/*.conf installer/mac/GeoServer.app/Contents/Info.plist
popd > /dev/null

pushd src > /dev/null

# build the release
if [ -z $SKIP_BUILD ]; then
  echo "building release"
  mvn $MAVEN_FLAGS clean install -DskipTests -P release
  
  # build the javadocs
  mvn javadoc:aggregate

  # build the user docs
  pushd ../doc/en/user > /dev/null
  make clean html

  cd ../developer
  make clean html

  popd > /dev/null
fi

mvn $MAVEN_FLAGS assembly:attached

# copy over the artifacts
if [ ! -e $DIST_PATH ]; then
  mkdir -p $DIST_PATH
fi
dist=$DIST_PATH/$tag
if [ -e $dist ]; then
  rm -rf $dist
fi
mkdir $dist
mkdir $dist/plugins

artifacts=`pwd`/target/release
# bundle up mac and windows installer stuff
pushd release/installer/mac > /dev/null
zip -r $artifacts/geoserver-$tag-mac.zip *
popd > /dev/null
pushd release/installer/win > /dev/null
zip -r $artifacts/geoserver-$tag-win.zip *
popd > /dev/null

pushd $artifacts > /dev/null

# setup doc artifacts
if [ -e user ]; then
  unlink user
fi
if [ -e developer ]; then
  unlink developer
fi

ln -sf ../../../doc/en/user/build/html user
ln -sf ../../../doc/en/developer/build/html developer
htmldoc=geoserver-$tag-htmldoc.zip
if [ -e $htmldoc ]; then
  rm -f $htmldoc 
fi
zip -r $htmldoc user developer
unlink user
unlink developer

# clean up source artifact
if [ -e tmp ]; then
  rm -rf tmp
fi
mkdir tmp
src=geoserver-$tag-src.zip
unzip -d tmp $src
pushd tmp > /dev/null

set +e && find . -type d -name target -exec rm -rf {} \; && set -e
rm ../$src
zip -r ../$src *

popd > /dev/null
popd > /dev/null

echo "copying artifacts to $dist"
cp $artifacts/*-plugin.zip $dist/plugins
for a in `ls $artifacts/*.zip | grep -v plugin`; do
  cp $a $dist
done

# fire off mac and windows build machines
if [ -z $SKIP_INSTALLERS ]; then
  echo "starting installer jobs"
  start_installer_job $WIN_HUDSON $tag
  start_installer_job $MAC_HUDSON $tag
fi

# git commit changes on the release branch
pushd .. > /dev/null
git add . 
git commit $git_opts -m "updating version numbers and release notes for $tag" .
popd > /dev/null

popd > /dev/null

echo "build complete, artifacts available at $DIST_URL/$tag"
exit 0
