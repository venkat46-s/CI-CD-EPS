pipeline {
    agent any
    environment {
        SERVICE_NAME = "${params.ServiceName}"
        IMAGE = "${params.Image}"
        PORT = "${params.Port}"
        ENVIRONMENT = "${params.Environment}"
        NAMESPACE = "${params.Namespace}"
        ACCOUNTID = "${params.Account}"
        CLUSTER_NAME = "${params.Cluster}"

    }
    stages {
        stage('Checkout CD Repo') {
            steps {
                // Check out the CD Bitbucket repository
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']], // Replace with the correct branch if needed
                    userRemoteConfigs: [[
                        url: 'https://github.com/Dee/theonemoment.git', // Replace with your repo URL
                        // credentialsId: 'c70fd5a3-dbcc-45df-b4d6-098b46d25cc9' // Replace with your Bitbucket credentials ID
                    ]]
                ])
            }
        }
stage('Generate YAML Files') {
    steps {
        script {
            // Echo the values of the variables for validation before running deploy.sh
            echo "Service Name: ${SERVICE_NAME}"
            echo "Image: ${IMAGE}"
            echo "Port: ${PORT}"
            echo "Environment: ${ENVIRONMENT}"
            echo "Namespace: ${NAMESPACE}"
            echo "Account ID: ${ACCOUNTID}"

            // Ensure deploy.sh has execute permission before running it
            sh """
              chmod +x ./eks/bin/deploy.sh
              ./eks/bin/deploy.sh ${SERVICE_NAME} ${IMAGE} ${PORT} ${ENVIRONMENT}  ${NAMESPACE} ${ACCOUNTID} 
            """
        }
    }
}


        stage('Login to AWS and Apply YAML') {
            steps {
                script {
                    // Determine AWS credentials based on the environment
                    def awsCredentialsId = ''
                    if (params.Environment in ['alpha']) {
                        awsCredentialsId = 'cdeksadmalpha'
                    } else if (params.Environment in ['stg', 'uat', 'dev', 'test']) {
                        awsCredentialsId = 'cdeksadmnp'
                    }

                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: awsCredentialsId, // AWS credentials based on environment
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]]) {
                        // Login to AWS and update kubeconfig with the cluster name
                        sh """
                        aws eks update-kubeconfig --name ${CLUSTER_NAME} --region ap-south-1
                        kubectl apply -f ./eks/templates/updated.yaml --namespace ${NAMESPACE}
                        """
                    }
                }
            }
        }
    }
}
