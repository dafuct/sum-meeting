import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private readonly STORAGE_TYPE = 'localStorage'; // Can be 'localStorage' or 'sessionStorage'

  constructor() { }

  /**
   * Gets a value from storage.
   */
  get(key: string): string | null {
    try {
      const storage = this.getStorage();
      return storage.getItem(key);
    } catch (error) {
      console.error('Storage get error:', error);
      return null;
    }
  }

  /**
   * Sets a value in storage.
   */
  set(key: string, value: string, permanent: boolean = true): void {
    try {
      const storage = permanent ? localStorage : sessionStorage;
      storage.setItem(key, value);
    } catch (error) {
      console.error('Storage set error:', error);
    }
  }

  /**
   * Removes a value from storage.
   */
  remove(key: string): void {
    try {
      localStorage.removeItem(key);
      sessionStorage.removeItem(key);
    } catch (error) {
      console.error('Storage remove error:', error);
    }
  }

  /**
   * Clears all storage.
   */
  clear(): void {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (error) {
      console.error('Storage clear error:', error);
    }
  }

  /**
   * Gets an object from storage.
   */
  getObject<T>(key: string): T | null {
    try {
      const value = this.get(key);
      return value ? JSON.parse(value) as T : null;
    } catch (error) {
      console.error('Storage getObject error:', error);
      return null;
    }
  }

  /**
   * Sets an object in storage.
   */
  setObject<T>(key: string, value: T, permanent: boolean = true): void {
    try {
      const serialized = JSON.stringify(value);
      this.set(key, serialized, permanent);
    } catch (error) {
      console.error('Storage setObject error:', error);
    }
  }

  /**
   * Checks if a key exists in storage.
   */
  exists(key: string): boolean {
    return this.get(key) !== null;
  }

  /**
   * Gets all keys from storage.
   */
  getKeys(): string[] {
    try {
      const keys: string[] = [];
      const storage = this.getStorage();
      
      for (let i = 0; i < storage.length; i++) {
        const key = storage.key(i);
        if (key) {
          keys.push(key);
        }
      }
      
      return keys;
    } catch (error) {
      console.error('Storage getKeys error:', error);
      return [];
    }
  }

  /**
   * Gets storage size in bytes.
   */
  getSize(): number {
    try {
      const storage = this.getStorage();
      let total = 0;
      
      for (let key in storage) {
        if (storage.hasOwnProperty(key)) {
          total += storage[key].length + key.length;
        }
      }
      
      return total;
    } catch (error) {
      console.error('Storage getSize error:', error);
      return 0;
    }
  }

  /**
   * Gets the appropriate storage object.
   */
  private getStorage(): Storage {
    if (this.STORAGE_TYPE === 'sessionStorage') {
      return sessionStorage;
    }
    return localStorage;
  }

  /**
   * Checks if storage is available.
   */
  isStorageAvailable(type: 'localStorage' | 'sessionStorage' = this.STORAGE_TYPE as any): boolean {
    try {
      const storage = window[type];
      const testKey = '__storage_test__';
      storage.setItem(testKey, 'test');
      storage.removeItem(testKey);
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Gets storage usage information.
   */
  getStorageInfo(): { used: number; available: number; percentage: number } {
    const used = this.getSize();
    const available = 5 * 1024 * 1024; // 5MB estimate for most browsers
    const percentage = Math.round((used / available) * 100);

    return { used, available, percentage };
  }

  /**
   * Removes all keys that start with a prefix.
   */
  removeByPrefix(prefix: string): void {
    const keys = this.getKeys();
    keys.forEach(key => {
      if (key.startsWith(prefix)) {
        this.remove(key);
      }
    });
  }

  /**
   * Creates a storage key with namespace.
   */
  createKey(namespace: string, key: string): string {
    return `${namespace}_${key}`;
  }

  /**
   * Migrates data from one key to another.
   */
  migrateKey(oldKey: string, newKey: string, removeOld: boolean = true): void {
    const value = this.get(oldKey);
    if (value !== null) {
      this.set(newKey, value);
      if (removeOld) {
        this.remove(oldKey);
      }
    }
  }

  /**
   * Backs up storage to a string.
   */
  backup(): string {
    try {
      const data: { [key: string]: string } = {};
      const keys = this.getKeys();
      
      keys.forEach(key => {
        data[key] = this.get(key) || '';
      });
      
      return JSON.stringify(data);
    } catch (error) {
      console.error('Storage backup error:', error);
      return '';
    }
  }

  /**
   * Restores storage from a backup string.
   */
  restore(backupString: string): void {
    try {
      const data = JSON.parse(backupString);
      
      Object.keys(data).forEach(key => {
        this.set(key, data[key]);
      });
    } catch (error) {
      console.error('Storage restore error:', error);
    }
  }
}