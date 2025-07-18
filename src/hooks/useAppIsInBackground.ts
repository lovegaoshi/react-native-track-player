import { useState, useEffect, useRef } from 'react';
import { AppState, AppStateStatus, NativeEventSubscription } from 'react-native';

export function useAppIsInBackground() {
  const [state, setState] = useState<AppStateStatus>('active');
  const eventListener = useRef<NativeEventSubscription>(undefined);

  useEffect(() => {
    const onStateChange = (nextState: AppStateStatus) => {
      setState(nextState);
    };

    eventListener.current = AppState.addEventListener('change', onStateChange);

    return () => {
      eventListener.current?.remove();
    };
  }, []);
  return state === 'background';
}
