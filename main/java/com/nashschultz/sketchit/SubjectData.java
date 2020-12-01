package com.nashschultz.sketchit;

class SubjectData {
    String SubjectName;
    String topName;
    byte[] image;
    public SubjectData(String subjectName, byte[] image, String topName) {
        this.SubjectName = subjectName;
        this.image = image;
        this.topName = topName;
    }
}