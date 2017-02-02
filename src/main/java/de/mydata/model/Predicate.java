package de.mydata.model;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface Predicate<T> {
	
	boolean test(T in);
	
	default Predicate<T> and(Predicate<T> second) {
		return new Predicate<T>() {
			@Override
			public boolean test(T in) {
				return Predicate.this.test(in) && second.test(in);
			}
		};
	}
	
	default Predicate<T> or(Predicate<T> alternative) {
		return new Predicate<T>() {
			@Override
			public boolean test(T in) {
				return Predicate.this.test(in) || alternative.test(in);
			}
		};
	}
	
	default Predicate<T> xor(Predicate<T> xorAlternative) {
		return new Predicate<T>() {
			@Override
			public boolean test(T in) {
				return Predicate.this.test(in) ^ xorAlternative.test(in);
			}
		};
	}
}