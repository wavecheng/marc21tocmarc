package com.wavecheng.marc21tocmarc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.RecordImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class MarcHardcodeTransformer {

	private static Logger log = LoggerFactory.getLogger(MarcHardcodeTransformer.class);
	private int total;
	private int success;
	private int error;
	private Map<String,String> countryMap;
	
	public MarcHardcodeTransformer() {
		countryMap = getCountryMap();	
	}
	
	
	public int transform(String fileIn, String fileOutput) throws IOException {
		MarcReader reader = new MarcStreamReader(new FileInputStream(fileIn));
		MarcWriter writer = new MarcStreamWriter(new FileOutputStream(fileOutput),"utf-8");
        while(reader.hasNext()) {
        	Record record = reader.next();
        	total ++;
        	try {        	
	        	Record cmarc = new RecordImpl();
	        	List<VariableField> fieldsList = new ArrayList<>(30);
	        	handleLeader(record,cmarc);
	        	handleControlFields(record,fieldsList);
	        	handleDataFields(record,fieldsList);
	        	
	        	//sort and insert into marc
	        	fieldsList.sort(new MarcTagComparator());
	        	for(VariableField f : fieldsList) {
	        		cmarc.addVariableField(f);
	        	}
	        	log.info(cmarc.toString());
	        	writer.write(cmarc);
	        	success++;
	        	
        	}catch(Exception ex) {
        		error ++;
        		log.error("failed:" + ex);
        		log.error("failed:" + record.toString());
        	}
        	log.info("total=" + total + ",success=" + success + ",failed="+ error);
        } 
        writer.close();
		return -1;
	}

	
	private void handleControlFields(Record record, List<VariableField> fieldsList) {
		List<ControlField> cfs = record.getControlFields();
		for(ControlField cf : cfs) {
			switch(cf.getTag()) {
				case "001":
					fieldsList.add(cf);
					break;
				case "008":
					handle008(cf,fieldsList);
					break;
			}
		}
	}

	private void handleDataFields(Record record, List<VariableField> fieldsList) {
		List<DataField> dfs =  record.getDataFields();
		for(DataField df : dfs) {
			try {
			switch(df.getTag()) {
				case "020":
					handle020(df,fieldsList);
					break;
				case "043":
					df.setTag("660");
					log.debug("043 => 660:" + df.toString());
					fieldsList.add(df);
					break;
				case "050":
					df.setTag("680");
					df.setIndicator2(' ');
					log.debug("050 => 680:" + df.toString());
					fieldsList.add(df);
					break;
				case "082":
					df.setTag("676");
					df.setIndicator1(' ');
					df.setIndicator2(' ');
					df = transSubfield(df, '2', 'v');
					log.debug("082 => 676:" + df.toString());
					fieldsList.add(df);
					break;
				case "100":
				case "700":
					handle100and700(df, fieldsList);
					break;
				case "245":
					handle245(df,fieldsList);
					break;
				case "260":
					handle260(df,fieldsList);
					break;
				case "264":
					//not existing rule by Excel
					break;
				case "300":
					handle300(df,fieldsList);
					break;
				case "490":
					df.setTag("225");
					df.setIndicator1('1');
					log.debug("490 => 225:" + df.toString());
					fieldsList.add(df);
					break;
				case "504":
					handle504(df,fieldsList);
					break;
				case "505":
					df.setTag("327");
					df.setIndicator1(' ');
					df.setIndicator2(' ');
					log.debug("505 => 327:" + df.toString());
					fieldsList.add(df);
					break;
				case "520":
					df.setTag("330");
					df.setIndicator1(' ');
					df.setIndicator2(' ');
					log.debug("520 => 330:" + df.toString());
					fieldsList.add(df);
					break;	
				case "513":
				case "514":
				case "518":
				case "524":
				case "526":
				case "533":
				case "588":
				case "590":
					handle533and588and590(df,fieldsList);
					break;
				case "650":
					handle650(df,fieldsList);
					break;
				case "651":
					handle651(df,fieldsList);
					break;
				case "655":
					handle655(df,fieldsList);
					break;
				case "710":
				case "797":
					df.setIndicator1('0');
					df.setIndicator2('2');
					df.setTag("712");
					log.debug("710/797 => 712:" + df.toString());
					fieldsList.add(df);
					break;
				case "856":
					log.debug("856 => 856:" + df.toString());
					fieldsList.add(df);	
					break;
			}
			}catch(Exception ex) {
				log.error("handle data field ex:" + ex);
			}
		}
	}

	private void handle008(ControlField df, List<VariableField> fieldsList) {		
		String data = df.getData();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String $a = format.format(new Date());
		switch(data.charAt(6)) {
			case 'b': 
			case 'n':
			case 'q':
				$a += "f"; 
				break;
			case 'c': $a += "a"; break;
			case 'd': $a += "b"; break;
			case 'e': 
			case 'p':
			case 's':
			case 't':
				$a += "d"; 
				break;
			case 'i':
			case 'k':
			case 'm':
				$a += "g"; 
				break;
			case 'r': $a += "e"; break;
			case 'u': $a += "c"; break;
		}
		
		//pos:9-16
		if(data.substring(7, 10).equalsIgnoreCase(data.substring(11,14)))
			$a += data.substring(7,11);
		else
			$a += data.substring(7,15);
		
		//pos:17-20
		$a += "k  ";
		
		//pos:21-
		$a += "0engy01      b";
		
		DataField nwDf = buildDataField("100a", $a);
		fieldsList.add(nwDf);
		
		String nation = data.substring(15, 18);
		fieldsList.add(buildDataField("102a", countryMap.getOrDefault(nation, nation)));
		fieldsList.add(buildDataField("101a", data.substring(35, 38)));
	}


	private Map<String, String> getCountryMap() {
		Gson gson = new Gson();
		try {
			String file = getClass().getClassLoader().getResource("county-code-map.json").getPath();
			return (Map<String,String>)gson.fromJson(new FileReader(file), Map.class);
		} catch (Exception ex) {
			log.error("read count-code-map.json failed: " + ex);
		}
		return new HashMap<String, String>();
	}

	private void handle655(DataField df, List<VariableField> fieldsList) {
		DataField nwDf = new DataFieldImpl("610", '1',' ');
		for(Subfield sub: df.getSubfields()) {
			sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
			nwDf.addSubfield(sub);		
		}	
		log.debug("655 => 610:" + nwDf.toString());
		fieldsList.add(nwDf);	
	}


	private void handle650(DataField df, List<VariableField> fieldsList) {
		char indi2 = df.getIndicator2();
		String subject = "";
		switch(indi2) {
			case '0':	subject = "lc"; break;
			case '1':	subject = "lcc"; break;
			case '2':	subject = "mesh"; break;
			case '3':	subject = "nal"; break;			
			case '5':	subject = "cae"; break;
			case '6':	subject = "caf"; break;
			default:  break;
		}
		
		DataField nwDf = new DataFieldImpl("606", df.getIndicator1(),' ');
		df.addSubfield(new SubfieldImpl('2', subject));
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'z') {
				sub.setCode('y');
			}else if(sub.getCode() == 'v') {
				sub.setCode('x');
			}else if(sub.getCode() == 'y') {
				sub.setCode('z');
			}
			sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
			nwDf.addSubfield(sub);		
		}	
		log.debug("650 => 606:" + nwDf.toString());
		fieldsList.add(nwDf);	
	}

	private void handle651(DataField df, List<VariableField> fieldsList) {
		char indi2 = df.getIndicator2();
		String subject = "";
		switch(indi2) {
			case '0':	subject = "lc"; break;
			case '1':	subject = "lcc"; break;
			case '2':	subject = "mesh"; break;
			case '3':	subject = "nal"; break;			
			case '5':	subject = "cae"; break;
			case '6':	subject = "caf"; break;
			default:  break;
		}
		
		DataField nwDf = new DataFieldImpl("607", df.getIndicator1(),' ');
		df.addSubfield(new SubfieldImpl('2', subject));
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'z') {
				sub.setCode('y');
			}else if(sub.getCode() == 'v') {
				sub.setCode('x');
			}else if(sub.getCode() == 'y') {
				sub.setCode('z');
			}
			sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
			nwDf.addSubfield(sub);		
		}	
		log.debug("651 => 607:" + nwDf.toString());
		fieldsList.add(nwDf);	
	}
	
	private void handle533and588and590(DataField df, List<VariableField> fieldsList) {
		DataField nwDf = new DataFieldImpl("300", df.getIndicator1(),df.getIndicator2());
		for(Subfield sub: df.getSubfields()) {
			sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
			nwDf.addSubfield(sub);		
		}	
		log.debug("5xx => 300:" + nwDf.toString());
		fieldsList.add(nwDf);		
	}


	private void handle504(DataField df, List<VariableField> fieldsList) {
		DataField nwDf = new DataFieldImpl("320", df.getIndicator1(),df.getIndicator2());
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'a') {
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
				nwDf.addSubfield(sub);
			}else {
				nwDf.addSubfield(sub);
			}
		}	
		log.debug("504 => 320:" + nwDf.toString());
		fieldsList.add(nwDf);
		
	}


	private void handle300(DataField df, List<VariableField> fieldsList) {
		DataField nwDf = new DataFieldImpl("210", '1', ' ');
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'a') {
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {':','.',','}));
				nwDf.addSubfield(sub);
			}else if(sub.getCode() == 'b') {
				sub.setCode('c');
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
				nwDf.addSubfield(sub);
			}else {
				nwDf.addSubfield(sub);
			}
		}
		
		log.debug("260 => 210:" + nwDf.toString());
		fieldsList.add(nwDf);
		
	}


	private void handle260(DataField df, List<VariableField> fieldsList) {
		DataField nwDf = new DataFieldImpl("210", '1', ' ');
		
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'a') {
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {':',';',','}));
				nwDf.addSubfield(sub);
			}else if(sub.getCode() == 'b') {
				sub.setCode('c');
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {';',':',','}));
				nwDf.addSubfield(sub);
			}else if(sub.getCode() == 'c') {
				sub.setCode('d');
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
				nwDf.addSubfield(sub);
			}else {
				nwDf.addSubfield(sub);
			}
		}		
		log.debug("260 => 210:" + nwDf.toString());
		fieldsList.add(nwDf);
		
	}


	private void handle245(DataField df, List<VariableField> fieldsList) {
		DataField nwDf = new DataFieldImpl("200", '1', ' ');
		
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'h') {
				//to 204a and remove "[]"
				DataField df1 = buildDataField("204",'1',' ','a',sub.getData().replaceAll("(\\[|\\]|/)",""));
				log.debug("245h => 204a: " + df1.toString());
				fieldsList.add(df1);
			}else if(sub.getCode() == 'a') {
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {':','='}));
				nwDf.addSubfield(sub);
			}else if(sub.getCode() == 'b') {
				sub.setCode('e');
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'/'}));
				nwDf.addSubfield(sub);
			}else if(sub.getCode() == 'c') {
				sub.setCode('f');
				sub.setData(removeLastCharAndTrim(sub.getData(), new char[] {'.'}));
				nwDf.addSubfield(sub);
			}else {
				nwDf.addSubfield(sub);
			}
		}
		
		log.debug("245 => 200:" + nwDf.toString());
		fieldsList.add(nwDf);
	}


	private void handle100and700(DataField df, List<VariableField> fieldsList) {
		char indi1 = df.getIndicator1();	
		char indi2 = df.getIndicator2();
		DataField nwDf = new DataFieldImpl("700", indi1, indi2);
		
		for(Subfield sub: df.getSubfields()) {
			if(sub.getCode() == 'a') {
				String[] vs= sub.getData().split("(,|\\|)");
				nwDf.addSubfield(new SubfieldImpl('a', vs[0]));
				if(vs.length >1) {
					for(int i=1;i<vs.length; i++) {
						String data = vs[i].trim();
						if(data.charAt(data.length()-1) == '.')
							data = data.substring(0, data.length()-1);
						nwDf.addSubfield(new SubfieldImpl('b', data));
					}
				}
			}else if(sub.getCode() == 'e'){
				continue;
			}else {
				nwDf.addSubfield(sub);
			}
		}
		nwDf = transSubfield(nwDf, 'd', 'f');
		log.debug("100/700 => 700:" + nwDf.toString());
		fieldsList.add(nwDf);
	}


	//change subfield tag 
	private DataField transSubfield(DataField vf, char from, char to) {
		for(Subfield sf : vf.getSubfields()) {
			if(sf.getCode() == from)
				sf.setCode(to);
		}
		return vf;
	}
	
	private void handle020(DataField df, List<VariableField> fieldsList) {
		char indi1 = df.getIndicator1();	
		char indi2 = df.getIndicator2();
		DataField nwDf = new DataFieldImpl("010", indi1, indi2);
		
		for(Subfield sub : df.getSubfields()) {
			//handle 020$z=aaa(****) where *** to 010$b
			if(sub.getCode() == 'z' || sub.getCode() == 'a') {
				String data = sub.getData();
				int posStart = data.indexOf("(");
				int posEnd = data.indexOf(")");
				if(posStart != -1 && posEnd > 1) {
					sub.setData(data.substring(0, posStart).trim());
					nwDf.addSubfield(sub);
					nwDf.addSubfield(new SubfieldImpl('b', data.substring(posStart+1, posEnd)));
				}else {
					nwDf.addSubfield(sub);
				}
				nwDf.setIndicator1('1');
			}else {
				nwDf.addSubfield(sub);
			}
		}
		log.debug("020 => 010:" + nwDf.toString());
		fieldsList.add(nwDf);
	}

	private DataField buildDataField(String tagWithSub, String data) {
		return buildDataField(tagWithSub.substring(0, 3), ' ', ' ', tagWithSub.charAt(3), data);
	}
	private DataField buildDataField(String tag, char indi1, char indi2,char sub, String data) {
		DataField df = new DataFieldImpl(tag, indi1, indi2);
		df.addSubfield(new SubfieldImpl(sub, data));
		return df;  
	}
	
	private String removeLastCharAndTrim(String in,char[] removes) {
		String out = in.trim();
		
		for(char c : removes) {
			if(c == out.charAt(out.length()-1)) {
				out = out.substring(0, out.length()-1);
			}
		}
		return out.trim();
	}
	private void handleLeader(Record record, Record cmarc) {
		Leader leader = record.getLeader();
		leader.setRecordStatus('c');
		
		//char 7-8
		char []impl1 = leader.getImplDefined1();		
		//char 17-19 
		char []impl2 = leader.getImplDefined2();
		
		switch(impl1[0]) {
			case 'b': impl1[0] = 'a'; break;
			case 'd': impl1[0] = 'a'; break;
			case 'i': impl1[0] = 's'; break;
		}
		
		//char 19 ==> 8
		switch(impl2[2]) {
			case ' ': impl1[1] = '0'; break;
			case 'a': impl1[1] = '1'; break;
			case 'b': impl1[1] = '2'; break;
			case 'c': impl1[1] = '2'; break;
		}

		//char 17 => 17
		switch(impl2[0]){
			case '1': impl2[0] = ' '; break;
			case '2':
			case '3':
			case '4':
				impl2[0] = '1'; break;
			case '5':
			case '7':
			case 'u':
			case 'z':
				impl2[0] = '3'; break;
			case '8': 
				impl2[0] = '2'; break;			
		}
		
		//char 18 ==> 18
		switch(impl2[1]) {
			case 'a': impl2[1] = ' ';break;
			case 'c': impl2[1] = 'i';break;
			case 'i': impl2[1] = ' ';break;
			case 'u': impl2[1] = 'n';break;
		}
		
		//char 8 ==> char 19
		impl2[2] = ' ';
		leader.setImplDefined1(impl1);
		leader.setImplDefined2(impl2);
		cmarc.setLeader(leader);
	}

}
