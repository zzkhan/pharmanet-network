docker volume ls -q | while read volume
do
  # Check if the volume name is NOT "prometheus-grafana_grafana_storage" or "prometheus-grafana_prometheus_data"
  if [[ "$volume" != "prometheus-grafana_grafana_storage" && "$volume" != "prometheus-grafana_prometheus_data" ]]; then
    echo "Removing volume: $volume"
    # Remove the volume
    docker volume rm "$volume"
  else
    echo "Skipping volume: $volume"
  fi
done
