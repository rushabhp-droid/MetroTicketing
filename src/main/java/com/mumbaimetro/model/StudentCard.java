package com.mumbaimetro.model;

/**
 * Concession smart card for college and university students
 * Demonstrates: Inheritance, Super constructor invocation, Method Overriding
 */
public class StudentCard extends Card {
    private static final long serialVersionUID = 1L;
    private static final double MIN_BALANCE = 30.0;
    private static final double STUDENT_DISCOUNT_PERCENT = 0.20; // 20% discount

    private String collegeName;
    private String studentRollNo;

    public StudentCard(String cardId, String holderName, String contactNumber, double initialBalance,
                       String collegeName, String studentRollNo) {
        super(cardId, holderName, contactNumber, initialBalance);
        this.collegeName = (collegeName != null && !collegeName.trim().isEmpty()) ? collegeName.trim() : "General College";
        this.studentRollNo = studentRollNo != null ? studentRollNo.trim() : "N/A";
    }

    @Override
    public double calculateDiscount(double baseFare) {
        // Students always enjoy a flat 20% concession on metro travel
        return baseFare * STUDENT_DISCOUNT_PERCENT;
    }

    @Override
    public double getMinimumBalanceRequired() {
        return MIN_BALANCE;
    }

    @Override
    public String getCardType() {
        return "Student Concession Card (20% OFF)";
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getStudentRollNo() {
        return studentRollNo;
    }

    public void setStudentRollNo(String studentRollNo) {
        this.studentRollNo = studentRollNo;
    }

    @Override
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder(super.generateReceipt());
        sb.insert(sb.lastIndexOf("========="),
                String.format(" College      : %s\n Roll Number  : %s\n Concession   : 20%% Off\n",
                        collegeName, studentRollNo));
        return sb.toString();
    }
}
