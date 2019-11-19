package com.stackroute.nlpmicroservice.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HtmlService {
    private static final Logger LOGGER= LoggerFactory.getLogger(HtmlService.class);
    // This is for converting HTML document to text using jsoup.
    public String html2text(String html) {
        return Jsoup.parse(html).text();
    }
    public List<String> getAllParagraphs(String html) {
        Document doc=Jsoup.parse(html);
        Elements paragraphs = doc.getElementsByTag("p");
        List<String> paragraphList=new ArrayList<>();
        for (Element paragraph : paragraphs) {
          if(paragraph.hasText())
          { paragraphList.add(paragraph.text()); }
        }
        return paragraphList;
    }

    public void getAllLi(String html) {
        Document doc = Jsoup.parse(html);
        Elements liTags = doc.getElementsByTag("li");
        for (Element paragraph : liTags) {
            LOGGER.info("p.toString()");
        }
    }
}
