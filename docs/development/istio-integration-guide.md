# Istio Integration Guide

This guide provides developers with the necessary information to effectively work with Istio in the MCP Debate System.

## 1. Overview

Istio is a service mesh that provides a transparent way to manage traffic, enforce security, and gain observability for microservices.

## 2. Key Concepts

*   **Sidecar Proxy (Envoy):** Injected alongside your application containers to intercept all network traffic.
*   **Control Plane (Istiod):** Manages and configures the Envoy proxies.
*   **Gateway:** Manages inbound/outbound traffic at the edge of the mesh.
*   **VirtualService:** Defines routing rules for requests.
*   **DestinationRule:** Configures load balancing, connection pools, and outlier detection for services.
*   **PeerAuthentication:** Enforces mutual TLS (mTLS) for secure communication.
*   **AuthorizationPolicy:** Defines access control rules.

## 3. Deploying Services into the Mesh

Ensure your namespace is labeled for Istio injection:

```bash
kubectl label namespace <your-namespace> istio-injection=enabled --overwrite
```

New pods deployed in this namespace will automatically have the Envoy sidecar injected.

## 4. Traffic Management

### 4.1. Ingress Gateway

Define a `Gateway` to expose services to external traffic:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: my-gateway
  namespace: default
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        name: http
        protocol: HTTP
      hosts:
        - "*"
```

### 4.2. Routing with Virtual Services

Define a `VirtualService` to route traffic to your service:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-service-vs
  namespace: default
spec:
  hosts:
    - "*"
  gateways:
    - my-gateway
  http:
    - match:
        - uri:
            prefix: /my-service
      route:
        - destination:
            host: my-service.default.svc.cluster.local
            port:
              number: 8080
```

## 5. Resilience Patterns

### 5.1. Retries and Timeouts

Configure retries and timeouts in your `VirtualService`:

```yaml
http:
  - route:
      - destination:
          host: my-service.default.svc.cluster.local
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx,gateway-error,connect-failure
    timeout: 10s
```

### 5.2. Circuit Breakers

Configure circuit breakers using `DestinationRule`:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: my-service-dr
  namespace: default
spec:
  host: my-service.default.svc.cluster.local
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 100
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 60s
      maxEjectionPercent: 100
```

## 6. Security

### 6.1. Mutual TLS (mTLS)

Enforce strict mTLS for services in a namespace:

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
```

### 6.2. Authorization Policies

Control access to your services:

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: my-service-access
  namespace: default
spec:
  selector:
    matchLabels:
      app: my-service
  rules:
    - from:
        - source:
            principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
      to:
        - operation:
            methods: ["GET"]
```

## 7. Observability

Istio automatically provides metrics, traces, and logs. Use Prometheus, Grafana, Jaeger, and Kiali to visualize and analyze this data.

*   **Kiali:** Access the Kiali dashboard via `istioctl dashboard kiali` (once the Kiali pod is running).

