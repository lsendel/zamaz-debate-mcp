import React, { useState, useRef, useEffect } from 'react';
import { Loader2, ImageOff } from 'lucide-react';

interface LazyImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  src: string;
  alt: string;
  className?: string;
  placeholder?: React.ReactNode;
  errorFallback?: React.ReactNode;
  threshold?: number;
}

const LazyImage: React.FC<LazyImageProps> = ({
  src,
  alt,
  className = '',
  placeholder,
  errorFallback,
  threshold = 0.1,
  ...props
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isInView, setIsInView] = useState(false);
  const [hasError, setHasError] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          observer.disconnect();
        }
      },
      { threshold }
    );

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, [threshold]);

  const handleLoad = () => {
    setIsLoaded(true);
  };

  const handleError = () => {
    setHasError(true);
    setIsLoaded(true);
  };

  const defaultPlaceholder = (
    <div className={`flex items-center justify-center bg-gray-100 ${className}`}>
      <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
    </div>
  );

  const defaultErrorFallback = (
    <div className={`flex items-center justify-center bg-gray-100 ${className}`}>
      <div className="text-center">
        <ImageOff className="h-8 w-8 text-gray-400 mx-auto mb-2" />
        <p className="text-sm text-gray-500">Failed to load image</p>
      </div>
    </div>
  );

  if (hasError) {
    return <>{errorFallback || defaultErrorFallback}</>;
  }

  return (
    <div ref={imgRef} className={className}>
      {!isLoaded && (placeholder || defaultPlaceholder)}
      {isInView && (
        <img
          src={src}
          alt={alt}
          className={`${className} ${isLoaded ? 'opacity-100' : 'opacity-0'} transition-opacity duration-300`}
          onLoad={handleLoad}
          onError={handleError}
          {...props}
        />
      )}
    </div>
  );
};

export default LazyImage;