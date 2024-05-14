pipeline {
  options {
    ansiColor("xterm")
    buildDiscarder(logRotator(numToKeepStr: "20"))
    timeout(time: 3, unit: "HOURS")
    disableConcurrentBuilds()
  }

  triggers {
    cron("0 0 * * *")
  }

  stages {
    stage("Checkout") {
      steps {
        checkout scm
      }
    }

    stage("Install Dependencies") {
      steps {
        sh "pip install -r requirements.txt"
        sh "dbt deps"
      }
    }

    stage("Build") {
      steps {
        sh "dbt build --target prod"
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/**/*', allowEmptyArchive: true
    }
  }
}
