package mosaic.paramopt.macro;

import ij.macro.MacroConstants;
import ij.macro.Program;
import ij.macro.Tokenizer;

import java.util.Iterator;
import java.util.Stack;

public class ParameterizedMacroParser implements MacroConstants {
	
	private static ParameterizedMacroParser instance = null;
	public static final String INDENTATION_STRING = "  ";

	public static ParameterizedMacro parseMacro(String macro) {
		if (instance == null)
			instance = new ParameterizedMacroParser();
		
		return instance.parse(macro);
	}
	
	public ParameterizedMacroParser() {
		// Initialize the required data structures.
		
	}
	
	public void clean() {
		// Clean everything from the parsing.
		currentIndentation = 0;
		currentLineNumber = 1;
		curtoken = 0;
		tokens = null;
		pgm = null;
		pmacro = null;
	}
	
	private String getindentationString() {
		String result = "";
		// Add spaces for the indentation level.
		for (int i = 0; i < currentIndentation; i++)
			result += INDENTATION_STRING;
		return result;
	}
	
	private Program pgm;
	int[] tokens;
	int curtoken;
	private ParameterizedMacro pmacro;
	// Current indentation of the code.
	private int currentIndentation = 0;
	// Number of the current line of code
	private int currentLineNumber = 1;
	
	private ParameterizedMacro parse(String macro) {
		synchronized(this) {
			clean();
			pmacro = new ParameterizedMacro();
			Tokenizer tk = new Tokenizer();
			pgm = tk.tokenize(macro);
			tokens = pgm.getCode();
			if (tokens.length < 1)
				return null;
			while ((tokens[curtoken]&TOK_MASK) != EOF) {
				doStatement();	
			}
			return pmacro;
		}
	}
	
	private void doStatement() {
		switch(tokens[curtoken]&TOK_MASK) {
		case PREDEFINED_FUNCTION:
		case NUMERIC_FUNCTION:
		case STRING_FUNCTION:
		case USER_FUNCTION:
		case ARRAY_FUNCTION:
			doFunction(null);
			break;
		case EOF:
			curtoken++;
			return;
		case ';':
			// Create a new line of code after each semicolon.
			pmacro.addStaticCode(";\n" + getindentationString());
			currentLineNumber++;
			curtoken++;
			break;
		case '{':
			// Opening curly bracket represents the begin of a code block.
			// Create a new line and increase the current indentation.
			currentIndentation++;
			pmacro.addStaticCode("{\n" + getindentationString());
			currentLineNumber++;
			curtoken++;
			break;
		case '}':
			// Closing curly bracket represents the end of a code block.
			// Create a new line and decrease the current indentation.
			currentIndentation--;
			pmacro.addStaticCode("}\n" + getindentationString());
			currentLineNumber++;
			curtoken++;
			break;
		default:
			if ((tokens[curtoken]&TOK_MASK) <= 16) {
				doSpecialToken();
				curtoken++;
				break;
			}
			if (tokens[curtoken] == (tokens[curtoken] & TOK_MASK)) {
				pmacro.addStaticCode((char) tokens[curtoken] + " ");
			} else {
				pmacro.addStaticCode(pgm.decodeToken(tokens[curtoken]) + " ");
			}
			curtoken++;
			break;
		}
	}
	
	

	@SuppressWarnings("unused")
	protected void doFunction(Function parent) {
		Function function = null;
		try {
			int token = parent == null ? curtoken : parent.localtoken;
			String functionName = pgm.decodeToken(tokens[token]);
			function = new Function(pmacro, parent, functionName);
			function.localtoken = token + 1;
			
			if(tokens[function.localtoken] == '(') {
				function.addStaticCode(pgm.decodeToken(tokens[function.localtoken]));
				function.localtoken++;
				doParameter(function);
				while(tokens[function.localtoken] == ',') {
					function.addStaticCode(pgm.decodeToken(tokens[function.localtoken]));
					function.localtoken++;
					doParameter(function);
				}
				if(tokens[function.localtoken] == ')') {
					function.addStaticCode(pgm.decodeToken(tokens[function.localtoken]));
					function.commitToParent();
					curtoken = function.localtoken + 1;
					return;
				}
			}
			throw new NotParametrizedMethodException();
		} catch(NotParametrizedMethodException e) {
			if (function != null) {
				function.abortToParent();
				if (parent == null)
					// Increment the global token counter
					curtoken = function.localtoken; // + 1;
				else
					// Increment the token counter of the parent function.
					parent.localtoken = function.localtoken + 1;
			}
			else {
				// Add the current token to the static code of the parent.
				if (parent == null) {
					// There's no parent function so add the function name
					// to the macro code list and increment the global token
					// counter.
					pmacro.addStaticCode(pgm.decodeToken(tokens[curtoken]));
					// go to next token
					curtoken++;
				}
				else {
					// There is a parent funciton so add the current token to 
					// the code list of the parent function an increment the
					// token count of the parent function.
					parent.addStaticCode(pgm.decodeToken(tokens[parent.localtoken]));
					parent.localtoken++;
				}
				
			}
		}
	}
	
	protected void doParameter(Function function) throws 
			NotParametrizedMethodException {
		switch(tokens[function.localtoken]&TOK_MASK) {
		case NUMBER:
			// Current token is a number and therefore a potential parameter.
			// Get the value of the number, create a new unnamed parameter, set
			// the function name and add it to the local code list.
			double value =
				pgm.getSymbolTable()[tokens[function.localtoken] >> TOK_SHIFT].value;
			function.addParameter(currentLineNumber, value, null);
			function.localtoken++;
			break;
		case STRING_CONSTANT:
			// Current token is a string.
			// Check if it contains a parameter.
			doStringConstant(function);
			break;
		case NUMERIC_FUNCTION:
		case STRING_FUNCTION:
		case USER_FUNCTION:
		case PREDEFINED_FUNCTION:
		case ARRAY_FUNCTION:
			doFunction(function);
			break;
		case PI:
			function.addStaticCode("PI");
			function.localtoken++;
			break;
		default:
			// Expected a number or a string as argument of the function but
			// the current token is something else therefore abort the parsing
			// of this function and continue with the current token.
			throw new NotParametrizedMethodException();
		}
	}
	
	protected void doStringConstant(Function function) {
		String str = pgm.decodeToken(tokens[function.localtoken]);
		if(str.matches("^\".*\"$")) {
			// Add an opening quote for the string which has been removed by the
			// Tokenizer.
			function.addStaticCode("\"");
			String unquoted = str.substring(1, str.length()-1);
			String[] parts = unquoted.split(" ");
			for(int i=0; i<parts.length; i++) {
				if(i>0) {
					// Add an empty space to separate arguments in the string.
					function.addStaticCode(" ");
				}
				if(parts[i].matches("^[a-zA-Z]+=\\d+\\.?\\d*$")) {
					// The current part of the string matches the regular
					// expression looking for name=value pairs where value can
					// be parsed as a double.
					// Split the current part on the '=' character and create a
					// new named parameter with the name and the parsed value
					// and add it to the code list of the current function.
					String[] tmp = parts[i].split("=");
					function.addStaticCode(tmp[0] + "=");
					function.addParameter(currentLineNumber, 
							Double.parseDouble(tmp[1]), tmp[0]);
				} else if(parts[i].matches("^\\d+\\.?\\d*$")) {
					// The current part is just a number so use it as an unnamed
					// parameter.
					function.addParameter(currentLineNumber, 
							Double.parseDouble(parts[i]), null);
				} else {
					// The current part of the string does not match the regular
					// expression and is therefore not a valid name=value pair.
					// Thus add it as a string to the local code list.
					function.addStaticCode(parts[i]);
				}
			}
			// Add a closing quote for the string to the local code list because
			// quotation marks have been removed by the <code>Tokenizer</code>.
			function.addStaticCode("\"");
		} else {
			// It's not really a string.
			function.addStaticCode(str);
			function.localtoken++;
		}
		function.localtoken++;
	}
	
	private void doSpecialToken() {
		switch(tokens[curtoken]&TOK_MASK) {
		case PLUS_PLUS:
			pmacro.addStaticCode("++");
			break;
		case MINUS_MINUS:
			pmacro.addStaticCode("--");
			break;
		case EQ:
			pmacro.addStaticCode("==");
			break;
		case NEQ:
			pmacro.addStaticCode("!=");
			break;
		case GT:
			pmacro.addStaticCode(">");
			break;
		case GTE:
			pmacro.addStaticCode(">=");
			break;
		case LT:
			pmacro.addStaticCode("<");
			break;
		case LTE:
			pmacro.addStaticCode("<=");
			break;
		case PLUS_EQUAL:
			pmacro.addStaticCode("+=");
			break;
		case MINUS_EQUAL:
			pmacro.addStaticCode("-=");
			break;
		case MUL_EQUAL:
			pmacro.addStaticCode("*=");
			break;
		case DIV_EQUAL:
			pmacro.addStaticCode("/=");
			break;
		case LOGICAL_AND:
			pmacro.addStaticCode("&&");
			break;
		case LOGICAL_OR:
			pmacro.addStaticCode("||");
			break;
		case SHIFT_RIGHT:
			pmacro.addStaticCode(">>");
			break;
		case SHIFT_LEFT:
			pmacro.addStaticCode("<<");
			break;
		default:
		}
	}

	public class NotParametrizedMethodException extends Exception {
		private static final long serialVersionUID = -4251017558678881827L;
	}

	private class Function {
		
		private ParameterizedMacro pmacro;
		private Function parentFunciton;
		private Stack<ParameterSettings> parameterSettings;
		private Stack<Object> codeList;
		private final String name;
		protected int localtoken;
		
		public Function(ParameterizedMacro macro, Function parentFunciton,
				String name) {
			this.pmacro = macro;
			this.parentFunciton = parentFunciton;
			this.name = name;
			codeList = new Stack<Object>();
			parameterSettings = new Stack<ParameterSettings>();
			addStaticCode(name);
		}
		
		public void addStaticCode(String code) {
			if (code != null) {
				// Check if last element in list is a string.
				if (!codeList.isEmpty() && codeList.peek() instanceof String) {
					String lastString = (String) codeList.pop();
					// Last element in the list is a string so concatenate the last
					// element with the new code.
					codeList.push(lastString + "" + code);
				} else {
					// Last element in the list is not a string so add the new code
					// just as a new element.
					codeList.push(code);
				}
			}
		}
		
		public void addParameter(int lineNumber, double value, String name) {
			// Create a new parameter and its settings.
			Parameter parameter = new Parameter(value);
			ParameterSettings settings = new ParameterSettings(parameter, 
					this.name, name, lineNumber);
			// Add the new parameter to the code and the parameter settings to the 
			// corresponding list.
			codeList.push(parameter);
			parameterSettings.push(settings);
		}
		
		public void addParameter(Parameter parameter, ParameterSettings settings) {
			// Add the new parameter to the code and the parameter settings to the 
			// corresponding list.
			codeList.push(parameter);
			parameterSettings.push(settings);
		}
		
		public void commitToParent() {
			Iterator<ParameterSettings> it = parameterSettings.iterator();
			for (Object o : codeList)
				if (o instanceof String) {
					if (parentFunciton == null)
						pmacro.addStaticCode((String) o);
					else
						parentFunciton.addStaticCode((String) o);
				} else if (o instanceof Parameter) {
					if (parentFunciton == null)
						pmacro.addParameter((Parameter) o, it.next());
					else
						parentFunciton.addParameter((Parameter) o, it.next());
				}
		}

		public void abortToParent() {
			if (parentFunciton == null)
				// Add all code elements as static code to the macro code list
				// as there is no parent function.
				for (Object o : codeList)
					pmacro.addStaticCode(o.toString());
			else
				// Add all code elements as static code to the parent function.
				for (Object o : codeList)
					parentFunciton.addStaticCode(o.toString());
		}
		
	}
}
