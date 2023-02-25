#!/bin/bash

##
## Usage :
## - Set the GITHUB_TOKEN env variable to a valid Github token
##

WORK_DIR=`mktemp -d`

for db in postgresql snowflake bigquery databricks h2 redshift spanner mssqlserver
do
        wflow="database-"$db"-sql-generation-integration-test.yml"
        result_dir=$WORK_DIR/$db
        mkdir -p $result_dir
        result_zip=$result_dir/result.zip

        echo "+ Workflow : $wflow"

        latest_run=`curl -s -H "Accept: application/vnd.github+json" \
              -H "Authorization: Bearer $GITHUB_TOKEN" \
              -H "X-GitHub-Api-Version: 2022-11-28" \
              https://api.github.com/repos/finos/legend-engine/actions/workflows/$wflow/runs | jq -r .workflow_runs[].artifacts_url | head -1`

        if [ $? != 0 ] || [ -z $latest_run ]
        then
                echo "!! Skipping database .."
                continue
        fi

        echo "+ Run : $latest_run"

        latest_artifact=`curl -s -H "Accept: application/vnd.github+json" \
              -H "Authorization: Bearer $GITHUB_TOKEN" \
              -H "X-GitHub-Api-Version: 2022-11-28" \
              $latest_run | jq -r '.artifacts[] | select (.name == "test-results") | .archive_download_url'`

        if [ $? != 0 ] || [ -z $latest_artifact ];
        then
                echo "!! Skipping database .."
                continue
        fi

        echo "+ Artifact : $latest_artifact : Downloading to $result_zip"

        curl -s -L -H "Accept: application/vnd.github+json" \
              -H "Authorization: Bearer $GITHUB_TOKEN" \
              -H "X-GitHub-Api-Version: 2022-11-28" \
              $latest_artifact --output $result_zip

        if [ $? != 0 ]
        then
                echo "!! Skipping database .."
                continue
        fi

        unzip $result_zip -d $result_dir
done


echo "+ Downloaded results "
tree $WORK_DIR