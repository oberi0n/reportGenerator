package com.example.reportgenerator.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportSearchController {
    private final ReportSearchService searchService;

    public ReportSearchController(ReportSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<ReportSearchResult> search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo) {
        return searchService.search(query, dateFrom, dateTo);
    }
}
