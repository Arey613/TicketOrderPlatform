# Minikube Local Deployment

Local Kubernetes manifests for running TicketOrderPlatform on Minikube.

## Prerequisites

- Minikube is running.
- `kubectl` points at the Minikube context.
- Images are built inside Minikube's Docker environment.

```sh
eval "$(minikube docker-env)"
make docker-build
docker build \
  -f database/java/migrations/ticket-order-db-migrations/Dockerfile \
  --build-arg MIGRATION_MODULE_PATH=database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations \
  -t ticket-order-transactional-migrations .
docker build \
  -f database/java/migrations/ticket-order-db-migrations/Dockerfile \
  --build-arg MIGRATION_MODULE_PATH=database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations \
  -t ticket-order-analytical-migrations .
```

Docker commands require task-specific permission before use.

## Deploy

Apply PostgreSQL first and wait for it to be ready:

```sh
kubectl apply -k infrastructure/kubernetes/minikube-local/postgres
kubectl -n ticket-order-platform-local rollout status statefulset/ticket-order-postgres
```

Run both independent migration jobs:

```sh
kubectl apply -k infrastructure/kubernetes/minikube-local/migrations
kubectl -n ticket-order-platform-local wait --for=condition=complete job/ticket-order-transactional-migrations --timeout=180s
kubectl -n ticket-order-platform-local wait --for=condition=complete job/ticket-order-analytical-migrations --timeout=180s
```

Deploy the API and web app:

```sh
kubectl apply -k infrastructure/kubernetes/minikube-local/apps
kubectl -n ticket-order-platform-local rollout status deployment/ticket-order-api
kubectl -n ticket-order-platform-local rollout status deployment/ticket-order-web
```

Open the web app:

```sh
minikube service -n ticket-order-platform-local ticket-order-web
```

## Cleanup

```sh
kubectl delete namespace ticket-order-platform-local
```

## Notes

- Migration jobs wait for PostgreSQL only; they do not depend on each other.
- Deploy the API only after both migration jobs complete.
- The web service uses a `NodePort` for convenient Minikube access.
- PostgreSQL uses a local `PersistentVolumeClaim`; deleting the namespace removes local database state.
