package io.jenetics.incubator.parser;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

public class BnfTokenizerTest {

	private static final String GRAMMAR_STRING = """
		<expr> ::= ( <expr> <op> <expr> ) | <num> | <var> | ( <expr> <op> <expr> )
		<op>   ::= '+' | - | '*' | / | '('
		<var>  ::= x | y
		<num>  ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
		""";

	@Test
	public void tokenize() {
		final var tokenizer = new BnfTokenizer(GRAMMAR_STRING);
		final List<Token> tokens = tokenizer.tokens().toList();

		final var string = tokens.stream()
			.map(t -> t.toString())
			.collect(Collectors.joining(" "));

		System.out.println(string);
	}

}
