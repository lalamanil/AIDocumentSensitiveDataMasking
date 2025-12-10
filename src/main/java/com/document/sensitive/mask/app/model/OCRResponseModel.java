package com.document.sensitive.mask.app.model;

import java.util.List;
import java.util.Map;

/**
 * @author lalamanil
 **/
public class OCRResponseModel {

	private String rawText;

	private Map<Integer, List<TokenModel>> pageTokenMap;

	public String getRawText() {
		return rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public Map<Integer, List<TokenModel>> getPageTokenMap() {
		return pageTokenMap;
	}

	public void setPageTokenMap(Map<Integer, List<TokenModel>> pageTokenMap) {
		this.pageTokenMap = pageTokenMap;
	}

}
