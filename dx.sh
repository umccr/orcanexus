nexus_rule () {
cat <<EOF
01. let keep the kiss principle for the dx script
02. you may directly execute each function wrapped commands
EOF
}

nexus_key () {
  aws ssm get-parameter --name 'orcahouse-mgmt' --output text --with-decryption --query 'Parameter.Value' > ~/.ssh/orcahouse-mgmt # pragma: allowlist secret
  chmod 600 ~/.ssh/orcahouse-mgmt
  ls -l ~/.ssh/orcahouse-mgmt
}

nexus_instance () {
  aws ec2 describe-instances \
    --filters 'Name=tag:Name,Values=orcahouse-mgmt-*' \
    --output text \
    --query 'Reservations[*].Instances[*].InstanceId'
}

nexus_endpoint () {
  # backend should only need to access to the reader endpoint
  aws rds describe-db-clusters --db-cluster-identifier orcanexus-dev --query "DBClusters[0].ReaderEndpoint" --output text
}

nexus_tunnel () {
  ssh -f -N -L 127.0.0.1:5432:"$(nexus_endpoint)":5432 \
    ubuntu@"$(nexus_instance)" -i ~/.ssh/orcahouse-mgmt \
    -o ProxyCommand='aws ec2-instance-connect open-tunnel --instance-id %h'
}

nexus_tunnel_fg () {
  # keep tunnel in the foreground, ctrl+c to end the tunnel session
  ssh -v -N -L 127.0.0.1:5432:"$(nexus_endpoint)":5432 \
    ubuntu@"$(nexus_instance)" -i ~/.ssh/orcahouse-mgmt \
    -o ProxyCommand='aws ec2-instance-connect open-tunnel --instance-id %h'
}

nexus_nc () {
  nc -vz 127.0.0.1 5432
}

nexus_status () {
  # ps aux | grep '[s]sh'
  # ps aux | grep '[o]rcahouse-mgmt'
  ps aux | grep '[o]pen-tunnel'
}

nexus_stop () {
  # kill <PID>
  pkill -f "open-tunnel"
}

nexus_forward () {
  aws ssm start-session \
    --target "$(nexus_instance)" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"portNumber\":[\"5432\"],\"localPortNumber\":[\"5432\"],\"host\":[\"$(nexus_endpoint)\"]}"
}

nexus_env () {
  aws ssm get-parameter --name '/orcanexus/env/dev' --output text --query 'Parameter.Value' > .env
	echo env ready
}

nexus_cred () {
  local script_dir
  local env_file

  script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
  env_file="${script_dir}/.env"

  if [[ ! -f "$env_file" ]]; then
    echo "Missing environment file: $env_file" >&2
    return 1
  fi

  # Export variables loaded from .env.
  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a

  : "${AWS_REGION:?AWS_REGION is required}"
  : "${DB_HOST:?DB_HOST is required}"
  : "${DB_NAME:?DB_NAME is required}"
  : "${DB_USER:?DB_USER is required}"

  export DB_PORT="${DB_PORT:-5432}"

  env | grep '^DB_'
}

nexus_pass () {
  # Generate a short-live db IAM auth token from RDS Aurora endpoint.
  # Typically you should not require to export this env var as we are leveraging AWS SDK for Java JDBC wrapper.
  # The wrapper should auto-refresh IAM auth token as needed upon the framework managed connection pool signal.
  DB_PASS="$(aws rds generate-db-auth-token \
    --hostname "$DB_HOST" \
    --port "$DB_PORT" \
    --region "$AWS_REGION" \
    --username "$DB_USER")"
  export DB_PASS
}

nexus_psql () {
  psql "host=$DB_HOST hostaddr=127.0.0.1 port=$DB_PORT dbname=$DB_NAME user=$DB_USER password=$DB_PASS sslmode=require"
}

nexus_clean () {
  unset DB_HOST DB_PORT DB_NAME DB_USER DB_PASS
  env | grep DB_
}

nexus_jooq () {
  mvn -pl orcanexus-jooq generate-sources -Pgenerate-jooq -Ddb.user="$DB_USER" -Ddb.password="$DB_PASS"
}
