package com.attask.jenkins;

import java.lang.reflect.Field;

/**
 * User: Joel Johnson
 * Date: 6/25/12
 * Time: 11:56 AM
 */
public class ReflectionUtils {

	/**
	 * Set's the value of the given field via reflection.
	 * @param instance
	 * @param fieldName
	 * @param value
	 */
	public static void setField(Object instance, String fieldName, Object value) {
		try {
			Field disabledField = findField(instance, fieldName);
			disabledField.set(instance, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e); //shouldn't happen
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the value of the given field via reflection.
	 * @param type
	 * @param instance
	 * @param fieldName
	 * @param <T>
	 * @return
	 */
	public static <T> T getField(Class<T> type, Object instance, String fieldName) {
		try {
			Field field = findField(instance, fieldName);
			return (T)field.get(instance);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e); //shouldn't happen
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	private static Field findField(Object instance, String fieldName) throws NoSuchFieldException {
		Class<?> projectClass = instance.getClass();
		Field disabledField = null;
		while(!Object.class.equals(projectClass)) {
			try {
				disabledField = projectClass.getDeclaredField(fieldName);
				disabledField.setAccessible(true);
				return disabledField;
			} catch (NoSuchFieldException ignore) {
				//doesn't have the declared field, go on to the next one
			}

			projectClass = projectClass.getSuperclass();
		}

		throw new NoSuchFieldException(fieldName);
	}
}
