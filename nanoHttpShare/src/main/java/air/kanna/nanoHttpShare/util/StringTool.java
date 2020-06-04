package air.kanna.nanoHttpShare.util;

public class StringTool {

	public static boolean isNullString(String str){
		return str == null || str.length() <= 0;
	}
	
	public static boolean isAllSpacesString(String str){
		if(isNullString(str)){
			return true;
		}
		
		int idx = 0;
		for(; idx<str.length(); idx++){
			char ch = str.charAt(idx);
			if(!isSpaceChar(ch)){
				break;
			}
		}
		
		if(idx < str.length()){
			return false;
		}
		
		return true;
	}
	
	public static boolean isSpaceChar(char ch){
		return ch == ' ' //半角空格
				|| ch =='\t' //制表符
				|| ch == 0x0A //回车
				|| ch == 0x0D //换行
				|| ch == (char)0x3000 //全角空格
				|| ch == 0xA0; //不知道什么格式的空格
	}
	
	public static String stringTrimEx(String str){
		if(isNullString(str)){
			return str;
		}
		
		int idx = 0;
		for(; idx<str.length(); idx++){
			char ch = str.charAt(idx);
			if(!isSpaceChar(ch)){
				break;
			}
		}
		if(idx < str.length()){
			str = str.substring(idx);
		}else{
			return "";
		}
		
		idx = str.length() - 1;
		for(; idx>=0; idx--){
			char ch = str.charAt(idx);
			if(!isSpaceChar(ch)){
				break;
			}
		}
		
		return str.substring(0, (idx + 1));
	}
}
