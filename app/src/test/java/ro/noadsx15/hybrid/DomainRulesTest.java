package ro.noadsx15.hybrid;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class DomainRulesTest {@Test public void adblock(){assertEquals("ads.example.com",DomainRules.parse("||ads.example.com^"));}@Test public void hosts(){assertEquals("ads.example.com",DomainRules.parse("0.0.0.0 ads.example.com"));}@Test public void suffix(){assertTrue(DomainRules.matches("a.ads.example.com",new HashSet<>(Arrays.asList("ads.example.com"))));}@Test public void sibling(){assertFalse(DomainRules.matches("goodexample.com",new HashSet<>(Arrays.asList("example.com"))));}}
