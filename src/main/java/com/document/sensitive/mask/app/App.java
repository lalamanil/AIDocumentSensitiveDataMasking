package com.document.sensitive.mask.app;

import com.document.sensitive.mask.app.utility.DocumentOCR;

/**
 * @author lalamanil
 */
public class App {
	public static void main(String[] args) {

		String filePath = "/Users/lalamanil/Documents/Anil LALAM/canadavisaphase2/BankStatements/eStmt_2023-11-06.pdf";

		DocumentOCR.performMasking(filePath);

	}
}
