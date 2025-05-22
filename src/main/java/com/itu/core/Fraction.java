package com.itu.core;

public class Fraction {
    private int numerator;
    private int denominator;

    public Fraction(int numerator, int denominator) {
        if (denominator == 0) throw new IllegalArgumentException("Denominator cannot be zero");
        if (denominator < 0) {
            numerator *= -1;
            denominator *= -1;
        }
        int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
        this.numerator = numerator / gcd;
        this.denominator = denominator / gcd;
    }

    public Fraction(int number) {
        this(number, 1);
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    //* ================ ARITHMETIC OPERATIONS ================
    public Fraction add(Fraction other) {
        int newDenominator = this.denominator * other.denominator;
        int newNumerator = this.numerator * other.denominator + other.numerator * this.denominator;
        return new Fraction(newNumerator, newDenominator);
    }

    public Fraction subtract(Fraction other) {
        return this.add(new Fraction(-other.numerator, other.denominator));
    }

    public Fraction multiply(Fraction other) {
        return new Fraction(
            this.numerator * other.numerator,
            this.denominator * other.denominator
        );
    }

    public Fraction divide(Fraction other) {
        return this.multiply(new Fraction(other.denominator, other.numerator));
    }

    //* ================ COMPARISON OPERATIONS ================
    public int compareTo(Fraction other) {
        int crossProduct1 = this.numerator * other.denominator;
        int crossProduct2 = other.numerator * this.denominator;
        return Integer.compare(crossProduct1, crossProduct2);
    }

    public boolean equals(Fraction other) {
        return this.numerator == other.numerator && 
               this.denominator == other.denominator;
    }

    public boolean isZero() {
        return numerator == 0;
    }

    public boolean isPositive() {
        return numerator > 0;
    }

    //* ================ UTILITY METHODS ================
    @Override
    public String toString() {
        if (denominator == 1) return String.valueOf(numerator);
        return numerator + "/" + denominator;
    }

    public double toDouble() {
        return (double) numerator / denominator;
    }

    public boolean isInteger() {
        return (getDenominator() == 1);
    }

    public Fraction floor() {
        double dValue = this.toDouble();
        return new Fraction((int)Math.floor(dValue));
    }

    // Getters (optional)
    public int getNumerator() { return numerator; }
    public int getDenominator() { return denominator; }
}