import { ActionReducerMap, MetaReducer } from '@ngrx/store';

// Simple environment object for now
const environment = {
  production: false
};

export interface State {
  // Add your state properties here
}

export const reducers: ActionReducerMap<State> = {
  // Add your reducers here
};

export const metaReducers: MetaReducer<State>[] = !environment.production ? [] : [];