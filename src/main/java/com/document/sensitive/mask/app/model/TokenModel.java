package com.document.sensitive.mask.app.model;

/**
 * @author lalamanil
 **/
public class TokenModel {

	private int pageNumber;

	private String word;

	private int startIndex;

	private int endIndex;

	private float minX;

	private float minY;

	private float maxX;

	private float maxY;

	private float pageWidth;

	private float pageHeight;

	public TokenModel() {

	}

	public TokenModel(int pageNumber, String word, int startIndex, int endIndex, float minX, float minY, float maxX,
			float maxY, float pageWidth, float pageHeight) {
		this.pageNumber = pageNumber;
		this.word = word;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;

	}

	public float getPageWidth() {
		return pageWidth;
	}

	public void setPageWidth(float pageWidth) {
		this.pageWidth = pageWidth;
	}

	public float getPageHeight() {
		return pageHeight;
	}

	public void setPageHeight(float pageHeight) {
		this.pageHeight = pageHeight;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public float getMinX() {
		return minX;
	}

	public void setMinX(float minX) {
		this.minX = minX;
	}

	public float getMinY() {
		return minY;
	}

	public void setMinY(float minY) {
		this.minY = minY;
	}

	public float getMaxX() {
		return maxX;
	}

	public void setMaxX(float maxX) {
		this.maxX = maxX;
	}

	public float getMaxY() {
		return maxY;
	}

	public void setMaxY(float maxY) {
		this.maxY = maxY;
	}

}
