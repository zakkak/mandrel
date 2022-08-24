#!/bin/bash
# Import graal stats into collector
#
# Usage:
#     DIR=quarkus/ TAG=gha-jdk11-mandrel-22.3.0-dev URL=http://127.0.0.1:8080/api/v1/image-stats bash import_stats.sh
#

#set -e

usage() {
  local missing="$1"
  echo "Error: '$missing' must be specified!" 1>&2
  echo 1>&2
  echo "usage:" 1>&2
  echo "  DIR=quarkus/ TAG=gha-jdk11-mandrel-22.3.0-dev TOKEN=<token> URL=http://127.0.0.1:8080/api/v1/image-stats bash import_stats.sh" 1>&2
  exit 1   
}

if [ "${DIR}_" == "_" ]; then
  usage "DIR"
elif [ "${TAG}_" == "_" ]; then
  usage "TAG"
elif [ "${URL}_" == "_" ]; then
  usage "URL"
elif [ "${TOKEN}_" == "_" ]; then
  usage "TOKEN"
fi

for bs in $(find $DIR -name \*build-output-stats.json); do
  f=$(echo "$bs" | sed 's/\(.*\)-build-output-stats\.json/\1/g')
  d=$(dirname $bs)
  ts="${f}-timing-stats.json"
  # import the stat
  stat_id=$(curl -s -w '\n' -H "Content-Type: application/json" \
            -H "token: $TOKEN" --post302 --data "@$(pwd)/$bs" "$URL/import?t=$TAG" | jq .id)
  # update timing info
  curl -s -w '\n' -H "Content-Type: application/json" -H "token: $TOKEN" \
	  -X PUT --data "@$ts" "$URL/$stat_id" > /dev/null
done
