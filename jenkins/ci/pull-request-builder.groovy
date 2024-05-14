pipeline {
  options {
    ansiColor("xterm")
    buildDiscarder(logRotator(numToKeepStr: "20"))
    timeout(time: 1, unit: "HOURS")
  }

  triggers {
    pullRequest()
  }

  stages {
    stage("Setup") {
      steps {
        sshagent (credentials: [SSH_CREDENTIALS_ID]) {
          sh "git fetch --all"
          sh "pip install -r requirements.txt"
          sh "dbt deps"

          script {
            CHANGED_SQL = get_changed_sql()
            SQLFLUFF_FILES = get_sqlfluff_files(CHANGED_SQL)
            CHANGED_MODELS = get_changed_models(CHANGED_SQL)
          }
        }
      }
    }

    stage("Test") {
      steps {
        sh "dbt build --target ci --select ${CHANGED_MODELS} --full-refresh"
      }
    }


    stage("Lint") {
      steps {
        sh "sqlfluff lint --target ci ${SQLFLUFF_FILES}"
      }
    }

  }

  post {
    always {
      archiveArtifacts artifacts: "target/**/*", allowEmptyArchive: true
    }
  }
}


def get_changed_sql() {
  command = "bash -c 'git --no-pager diff --name-only --diff-filter=d origin/duckdb -- models/**/*.sql'"

  status = sh(script: command, returnStatus: true)
  if (status != 0) {
    return ""
  }

  return sh(script: command, returnStdout: true).trim()
}

def get_sqlfluff_files(sql_files) {
  return sql_files.replaceAll("\n", " ")
}

def get_changed_models(sql_files) {
  models = []
  String[] files_list = sql_files.split("\n")

  for(String filename: files_list) {
    model_name = basename(filename)
    models << "@${model_name}"
  }

  return models.join(" ")
}

def basename(filename) {
  return sh(script: "basename $filename .sql", returnStdout: true).trim()
}
