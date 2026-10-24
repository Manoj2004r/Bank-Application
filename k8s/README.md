# Kubernetes Deployment — JavaBank

## Overview

This directory contains Kubernetes configuration for deploying and managing the JavaBank application.

## Deployment Components

- **Deployments:** Manage application pods and container images.
- **Services:** Provide network access to application workloads.
- **Configuration:** Supplies application settings where required.
- **Namespaces:** Organize application and platform resources.

## Deployment Checklist

Before deploying, verify:

1. Container image names and tags.
2. Container and Service ports.
3. Frontend-to-backend API connectivity.
4. Environment variables and application configuration.
5. Health probes and resource requests and limits.
6. Persistent storage requirements for application data.

## Monitoring

The project includes Prometheus and Grafana monitoring for Kubernetes resource metrics. Refer to the monitoring configuration and dashboards used in the deployed environment.

## Important Notes

Review all manifests before applying them. This documentation does not itself deploy the application. Persistent data must be configured separately if it needs to survive pod replacement.
