"""
Dependency injection container for the Kiro GitHub Integration system.

This module provides a simple but powerful dependency injection container
that manages service lifecycles and dependencies.
"""

import inspect
from collections.abc import Callable
from typing import Any, TypeVar

from .exceptions import ConfigurationError, ServiceError

T = TypeVar("T")


class ServiceContainer:
    """
    Dependency injection container for managing services.

    Supports:
    - Singleton and transient services
    - Automatic dependency resolution
    - Service factories
    - Configuration injection
    """

    def __init__(self):
        self._services: dict[type, tuple[Any, bool]] = {}
        self._singletons: dict[type, Any] = {}
        self._factories: dict[type, Callable] = {}
        self._config: dict[str, Any] = {}

    def register(self, interface: type[T], implementation: type[T] | Callable[..., T], singleton: bool = True) -> None:
        """
        Register a service implementation.

        Args:
            interface: The interface or base class
            implementation: The concrete implementation or factory
            singleton: Whether to create a single instance
        """
        if inspect.isclass(implementation):
            self._services[interface] = (implementation, singleton)
        else:
            # It's a factory function
            self._factories[interface] = implementation

    def register_instance(self, interface: type[T], instance: T) -> None:
        """Register an existing instance as a singleton."""
        self._singletons[interface] = instance
        self._services[interface] = (instance.__class__, True)

    def register_config(self, config: dict[str, Any]) -> None:
        """Register configuration values."""
        self._config.update(config)

    def get(self, interface: type[T]) -> T:
        """
        Get a service instance.

        Args:
            interface: The interface to resolve

        Returns:
            The service instance

        Raises:
            ServiceError: If service cannot be resolved
        """
        # Check if we have a singleton instance
        if interface in self._singletons:
            return self._singletons[interface]

        # Check if we have a factory
        if interface in self._factories:
            instance = self._factories[interface](self)
            if interface in self._services and self._services[interface][1]:
                self._singletons[interface] = instance
            return instance

        # Check if we have a registered service
        if interface not in self._services:
            raise ServiceError(f"No service registered for {interface.__name__}")

        implementation, is_singleton = self._services[interface]

        # Create instance
        instance = self._create_instance(implementation)

        # Store singleton if needed
        if is_singleton:
            self._singletons[interface] = instance

        return instance

    def _create_instance(self, implementation: type[T]) -> T:
        """Create an instance with dependency injection."""
        # Get constructor signature
        sig = inspect.signature(implementation.__init__)
        kwargs = {}

        for param_name, param in sig.parameters.items():
            if param_name == "self":
                continue

            # Skip if it has a default value
            if param.default != inspect.Parameter.empty:
                continue

            # Get type annotation
            param_type = param.annotation

            if param_type == inspect.Parameter.empty:
                # Check if it's a config parameter
                if param_name in self._config:
                    kwargs[param_name] = self._config[param_name]
                else:
                    raise ConfigurationError(f"Cannot resolve parameter '{param_name}' for {implementation.__name__}")
            else:
                # Try to resolve the dependency
                try:
                    kwargs[param_name] = self.get(param_type)
                except ServiceError:
                    # Check if it's in config
                    if param_name in self._config:
                        kwargs[param_name] = self._config[param_name]
                    else:
                        raise

        return implementation(**kwargs)

    def has(self, interface: type) -> bool:
        """Check if a service is registered."""
        return interface in self._services or interface in self._factories or interface in self._singletons

    def clear(self) -> None:
        """Clear all registrations."""
        self._services.clear()
        self._singletons.clear()
        self._factories.clear()
        self._config.clear()

    def create_scope(self) -> "ServiceContainer":
        """
        Create a scoped container.

        Scoped containers share service registrations but not singleton instances.
        """
        scope = ServiceContainer()
        scope._services = self._services.copy()
        scope._factories = self._factories.copy()
        scope._config = self._config.copy()
        return scope


# Global container instance
_container = ServiceContainer()


def get_container() -> ServiceContainer:
    """Get the global service container."""
    return _container


def register_services(container: ServiceContainer | None = None) -> None:
    """
    Register all default services.

    This function should be called during application startup.
    """
    if container is None:
        container = get_container()

    # Import implementations
    from ..implementations import (
        FernetEncryption,
        FileConfiguration,
        GitHubClient,
        JWTAuthentication,
        PrometheusMetrics,
        RBACAuthorization,
        RedisCache,
        SQLiteDatabase,
    )
    from .interfaces import (
        AuthenticationInterface,
        AuthorizationInterface,
        CacheInterface,
        ConfigurationInterface,
        DatabaseInterface,
        EncryptionInterface,
        GitHubClientInterface,
        MetricsInterface,
    )

    # Register implementations
    container.register(DatabaseInterface, SQLiteDatabase)
    container.register(GitHubClientInterface, GitHubClient)
    container.register(CacheInterface, RedisCache)
    container.register(MetricsInterface, PrometheusMetrics)
    container.register(AuthenticationInterface, JWTAuthentication)
    container.register(AuthorizationInterface, RBACAuthorization)
    container.register(EncryptionInterface, FernetEncryption)
    container.register(ConfigurationInterface, FileConfiguration)


# Decorator for dependency injection
def inject(func: Callable) -> Callable:
    """
    Decorator for automatic dependency injection.

    Example:
        @inject
        def process_webhook(github_client: GitHubClientInterface):
            # github_client will be automatically injected
            pass
    """
    sig = inspect.signature(func)

    def wrapper(*args, **kwargs):
        container = get_container()

        # Inject missing parameters
        for param_name, param in sig.parameters.items():
            if param_name in kwargs:
                continue

            param_type = param.annotation
            if param_type != inspect.Parameter.empty and container.has(param_type):
                kwargs[param_name] = container.get(param_type)

        return func(*args, **kwargs)

    return wrapper
