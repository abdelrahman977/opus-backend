package com.opus.backend.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.spire.xls.ExcelVersion;
import com.spire.xls.Workbook;
import com.spire.xls.Worksheet;
import com.spire.xls.WorksheetCopyType;


@SuppressWarnings("deprecation")
public class PowerExcelReader {
	String outputFileName;
	String primary_key;
	String filespath;
	String outpath;
	String dictionary_file;
	OPCPackage pkg;
	XSSFWorkbook wb;
	int numberOfCols = 0;
	final XSSFWorkbook newWorkbook = new XSSFWorkbook();  
	HashMap<Integer, String> regularExp;
	String [] columns;
	
	public PowerExcelReader(String primary_key,String outputFileName,String filespath ,String outpath,String dictionary_file) throws Exception{
		this.primary_key = primary_key;
		this.outputFileName = outputFileName;
		this.filespath = filespath;
		this.outpath = outpath;
		this.dictionary_file = dictionary_file;
		this.mergeFiles();
		this.pkg = OPCPackage.open(new File(this.filespath + outputFileName));
		this.wb = new XSSFWorkbook(pkg);	
		wb.removeSheetAt(wb.getSheetIndex("Evaluation Warning")); // THIS CONDITION IS BEC SPIRE LIBRARY ADD EXTRA SHEET
		this.regularExp = new HashMap<>(); //Columns needed for the aggregation
		ArrayList<String[]> dictionary = this.getDictionary(dictionary_file);
		columns = new String[dictionary.size()];
		for(int i = 0;i < dictionary.size();i ++) {
			columns[i] = dictionary.get(i)[0];
			this.regularExp.put(i,dictionary.get(i)[1]);
		}
	}
	
	public int[] getTableRowCol(int sheetNum) throws InvalidFormatException, IOException {	
		int[] tableRowCol = {-1, -1};
		Sheet sheet = this.wb.getSheetAt(sheetNum);
			Row r;
			outerloop:
			for(int rownum = 0; rownum < sheet.getLastRowNum() ; rownum ++) {	
				r = sheet.getRow(rownum);
				if(r != null) {
					for(int colnum = 0 ; colnum < r.getLastCellNum(); colnum ++) {
						Cell c = r.getCell(colnum, MissingCellPolicy.RETURN_BLANK_AS_NULL);
						if (c == null) {
					         // The spreadsheet is empty in this cell
					    } 
						else {
							for (Integer key : this.regularExp.keySet()) {
								if(c.toString().toLowerCase().matches(this.regularExp.get(key))) {
									tableRowCol[0] = rownum;
									tableRowCol[1] = colnum;
									numberOfCols = r.getLastCellNum();
									break outerloop;
								}
							}				    					    	  			    		  
						}
					}
				}
			}
		
		return tableRowCol;
	}
	public void excelCleansingTable() throws IOException, InvalidFormatException { /* export expected columns for calculations */   
		XSSFSheet newSheet;	
		for (int sheetNum = 0; sheetNum < wb.getNumberOfSheets(); sheetNum++) {
			/* assign beginning of the table in a sheet */
			Sheet sheet = this.wb.getSheetAt(sheetNum);
			int [] tableRowCol = this.getTableRowCol(sheetNum);
			int row = tableRowCol[0];
			int col = tableRowCol[1];
			newSheet = newWorkbook.createSheet("Cleansed_Sheet_" + (sheetNum + 1));
			int newRow = (row * -1); /* Normalize Row for new table; + 1 because the first row is for column names*/
			Row r = sheet.getRow(row);
			Cell c = r.getCell(col, MissingCellPolicy.RETURN_BLANK_AS_NULL);
			newSheet.createRow(0); // creating row for the column names values
			for (Integer key : this.regularExp.keySet()) { // Putting needed column names in the output excel
				 c = r.getCell(col, MissingCellPolicy.RETURN_BLANK_AS_NULL); 
				 newSheet.getRow(0).createCell(key).setCellValue(columns[key]); /* writing into the new excel */						
			}
			for(int i = col;i < numberOfCols;i ++) { /* looping on every column to check if it matches the regex */
					r = sheet.getRow(row);
					if(r != null) {
						c = r.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);							
							for (Integer key : this.regularExp.keySet()) {
								if(c != null) {		
									if(c.toString().toLowerCase().matches(this.regularExp.get(key))) {
										int currOutRow = row + 1;
										for(int j = row + 1; j < sheet.getLastRowNum() + 1;j++,currOutRow++) {
											r = sheet.getRow(j);
											if(r != null) {
												if(isRowEmpty(r)) {
													currOutRow--;
													continue;
												}
												if(newSheet.getRow(newRow + currOutRow) == null) { /* thats why we multiplied newRow by -1*/
													newSheet.createRow(newRow + currOutRow);
												}																					
												c = r.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);
												if(c != null) {	/* exporting needed table to new file */
													if(c.getCellType() == CellType.NUMERIC && HSSFDateUtil.isCellDateFormatted(c)) { /* ensuring date is exported correctly */
												          if(c.getNumericCellValue()<1) { // handling time cells
													          newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(c.getNumericCellValue() * 24);  /* writing into the new excel */							
												          }
												          else {
												        	  DateFormat df = new SimpleDateFormat("yyyy-MM-dd");				     
													          Date date = c.getDateCellValue();										        
													          newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(df.format(date));  /* writing into the new excel */							
												          }													  
													}
													else {
														if (c.getCellType() == CellType.FORMULA) { /* handling cells with formulas */
														    switch (c.getCachedFormulaResultType()) {
														        case BOOLEAN:
														        	newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(c.getStringCellValue()); /* writing into the new excel */ 
														            break;
														        case NUMERIC:
														        	if(HSSFDateUtil.isCellDateFormatted(c)){ /* ensuring date is exported correctly */
														        		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
																        Date date = c.getDateCellValue();										        
																        newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(df.format(date));  /* writing into the new excel */	
														        	}
														        	else {
															        	newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(c.getNumericCellValue()); /* writing into the new excel */ 
														        	}
														            break;
														        case STRING:
														        	newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(c.getRichStringCellValue()); /* writing into the new excel */ 
														            break;
															default:
																break;
														    }
														}
														else {
															newSheet.getRow(newRow + currOutRow).createCell(key).setCellValue(c.toString()); /* writing into the new excel */
														}										
													}													
												}
											}
										}
									}
								}						
							}
					}
			}
		}



		
	}

	public static boolean isRowEmpty(Row row) {
	    for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
	        Cell cell = row.getCell(c);
	        if (cell != null && cell.getCellType() != CellType.BLANK)
	            return false;
	    }
	    return true;
	}
	
	public int getNumberOfSheetAvaliable() {
		return this.wb.getNumberOfSheets();
	}
	public void close() throws IOException {	
		FileOutputStream fileOut = new FileOutputStream(outpath);  
		newWorkbook.write(fileOut);  
		//closing the Stream  
		fileOut.close();  
		//closing the workbook  
		newWorkbook.close();
		System.out.println("file exported successfully");
		pkg.close();
		FileUtils.deleteDirectory(new File(this.filespath));
	}
	public ArrayList<String[]> getDictionary (String dictionary_name) throws IOException{
    	File file = new File(this.dictionary_file);
    	ArrayList<String[]> out = new ArrayList<String[]>(); // Create an ArrayList object
    	String[] primary_key = {"primary_key",this.primary_key};
    	out.add(primary_key);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
        	if(line.charAt(0) == '*')
        		continue;
            out.add(line.split(","));
        }
        br.close();
	return out;
	}
	@SuppressWarnings("unchecked")
	public void mergeFiles() {
		Workbook output = new Workbook();
		//output.getWorksheets().clear();
		output.loadFromFile(this.filespath + this.outputFileName);
		File[] files = new File(this.filespath).listFiles();
		Workbook tempBook = new Workbook();
		for (File file : files) {
		    if (file.isFile() && !file.getName().equals(this.outputFileName)) {
	            tempBook.loadFromFile(this.filespath + file.getName());
	            for (Worksheet sheet : (Iterable<Worksheet>)tempBook.getWorksheets())
	            {
	            	output.getWorksheets().addCopy(sheet, WorksheetCopyType.CopyNames);
	            }
		    }
		  output.saveToFile(this.filespath + this.outputFileName, ExcelVersion.Version2007);
	      //output.saveToFile("MergeFiles.xlsx", ExcelVersion.Version2013);
		}

	}
}
