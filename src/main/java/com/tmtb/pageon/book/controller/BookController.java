package com.tmtb.pageon.book.controller;

import com.tmtb.pageon.book.model.BookVO;
import com.tmtb.pageon.book.service.BookService;
import com.tmtb.pageon.forum.model.ForumVO;
import com.tmtb.pageon.forum.service.ForumService;
import com.tmtb.pageon.review.model.ReviewVO;
import com.tmtb.pageon.review.service.ReviewService;
import com.tmtb.pageon.user.model.UserVO;
import com.tmtb.pageon.user.service.ProductService;
import com.tmtb.pageon.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class BookController {

    @Autowired
    private BookService service;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ForumService forumService;

    @Autowired
    private ProductService productService; // 캐싱을위한 product service생성

    @Autowired
    private UserService userService;

    // 목록 조회 (카테고리 및 검색 조건에 따라)
    @GetMapping("/books")
    public String selectAllBooks(Model model,
                                 @RequestParam(required = false) List<String> category,
                                 @RequestParam(required = false, defaultValue = "1") int cpage,
                                 @RequestParam(required = false, defaultValue = "20") int pageBlock,
                                 @RequestParam(required = false) String searchKey,
                                 @RequestParam(required = false) String searchWord,
                                 @RequestParam(required = false, defaultValue = "latest") String sortOrder) {
        // 테스트용 더미 사용자 ID
        String id = "tester2";
        log.info("테스트용 사용자 ID: {}", id);

        // id로 사용자 정보 조회 후 like_categories 가져오기
        UserVO user = userService.findById(id);
        List<String> likeCategories = Arrays.asList(user.getLike_categories().split(","));
        log.info("likeCategories: {}", likeCategories);
        model.addAttribute("likeCategories", likeCategories);

        // 중첩된 대괄호 제거 및 리스트 변환
        if (category != null && !category.isEmpty()) {
            category = category.stream()
                    .flatMap(cat -> Arrays.stream(cat.replaceAll("\\[|\\]", "").split(",\\s*")))
                    .distinct()
                    .collect(Collectors.toList());
        }

        log.info("selectAllBooks() category: {}, cpage: {}, pageBlock: {}, searchKey: {}, searchWord: {}, sortOrder: {}",
                category, cpage, pageBlock, searchKey, searchWord, sortOrder);

        List<BookVO> list;
        int totalRows;

        if (searchWord != null && !searchWord.isEmpty()) {
            if (category != null && !category.isEmpty()) {
                list = service.searchBooksInCategories(category, searchKey, searchWord, cpage, pageBlock, sortOrder);
                totalRows = service.getSearchTotalRowsInCategories(category, searchKey, searchWord);
            } else {
                list = service.searchBooks(searchKey, searchWord, cpage, pageBlock, sortOrder);
                totalRows = service.getSearchTotalRows(searchKey, searchWord);
            }
        } else if (category != null && !category.isEmpty()) {
            list = service.selectBooksByCategories(category, cpage, pageBlock, sortOrder);
            totalRows = service.getTotalRowsByCategories(category);
        } else {
            list = service.selectAllBooks(cpage, pageBlock, sortOrder);
            totalRows = service.getTotalRows();
        }

        int totalPageCount = (int) Math.ceil((double) totalRows / pageBlock);

        model.addAttribute("list", list);
        model.addAttribute("totalPageCount", totalPageCount);
        model.addAttribute("cpage", cpage);
        model.addAttribute("category", category);
        model.addAttribute("searchKey", searchKey);
        model.addAttribute("searchWord", searchWord);
        model.addAttribute("sortOrder", sortOrder);

        return "book/list";
    }

    // 상세 조회
    @GetMapping("/book/detail")
    public String selectOne(BookVO vo, Model model, HttpSession session) {
        log.info("/book/detail");
        log.info("vo: {}", vo);

        // 현재 책 정보 조회
        BookVO vo2 = service.selectOne(vo);
        log.info("vo2: {}", vo2);

        // 세션에서 사용자 ID 가져오기
        String id = (String) session.getAttribute("id");
        log.info("세션에서 가져온 사용자 ID: {}", id);

        // 최근 본 항목 추가
        List<Object> recentItems = productService.addRecentItem(id,vo2); // 캐싱을 위한 product service 생성 작성자:김규년
        log.info("최근 본 항목 리스트: {}", recentItems);

        model.addAttribute("vo2", vo2);

        // 동일한 카테고리의 유사한 책 조회
        List<BookVO> list = service.getLimitedBooksByCategory(vo2.getCategories(), 20, vo2.getItem_id());
        model.addAttribute("list", list);

        // 제목으로 리뷰와 토론 검색
        String bookTitle = vo2.getTitle();

        // 최대 5개의 리뷰와 토론 조회
        List<ReviewVO> reviewList = reviewService.searchListPageBlock("workTitle", bookTitle, 1, 20);
        List<ForumVO> forumList = forumService.searchForum("workTitle", bookTitle, 1, 20);

        model.addAttribute("reviewList", reviewList);
        model.addAttribute("forumList", forumList);
        log.info("동일한 카테고리의 유사한 책 리스트: {}", list);

        return "book/detail";
    }
    //  // 세션에서 사용자 ID 가져오기
    //        String id = (String) session.getAttribute("id");
    //        log.info("세션에서 가져온 사용자 ID: {}", id);
    //
    //        List<Object> recentItems = productService.addRecentItem(id,vo2); // 캐싱을 위한 product service 생성
    //        log.info("최근 본 항목 리스트: {}", recentItems);
    //이 내용은 사용자 최근 조회 목록을 위해 싱세조회api가 호출할때 발생하는 vo데이터들을 캐싱하기 위해서 짠 로직입니다
    //여러번 테스트를 해본결과 해당 기능에 문제가 없고 최근조회목록 조회기능에도 문제가 없는거 같습니다 만약 문제가 생긴다면 연락부탁드립니다.

    @GetMapping("/books/liked")
    public String selectBooksByLikeCategories(Model model, HttpSession session,
                                                  @RequestParam(required = false, defaultValue = "1") int cpage,
                                                  @RequestParam(required = false, defaultValue = "20") int pageBlock,
                                                  @RequestParam(required = false, defaultValue = "latest") String sortOrder) {
        // 세션에서 사용자 ID 가져오기
        String id = (String) session.getAttribute("id");
        log.info("세션에서 가져온 사용자 ID: {}", id);

        // id로 사용자 정보 조회 후 like_categories 가져오기
        UserVO user = userService.findById(id);
        List<String> likeCategories = Arrays.asList(user.getLike_categories().split(",\\s*"));
        log.info("likeCategories: {}", likeCategories);
        model.addAttribute("likeCategories", likeCategories);

        String filter = "prefer";
        model.addAttribute("filter", filter);

        if (likeCategories.isEmpty()) {
            log.info("No like_categories found for userId: {}", id);
            model.addAttribute("likedBooks", Collections.emptyList());
            model.addAttribute("totalPageCount", 0);
            return "book/list";
        }

        // 해당 카테고리에 맞는 목록 조회
        List<BookVO> list = service.selectBooksByCategories(likeCategories, cpage, pageBlock, sortOrder);
        int totalRows = service.getTotalRowsByCategories(likeCategories);
        int totalPageCount = (int) Math.ceil((double) totalRows / pageBlock);

        model.addAttribute("list", list);
        model.addAttribute("totalPageCount", totalPageCount);
        model.addAttribute("cpage", cpage);
        model.addAttribute("sortOrder", sortOrder);

        return "book/list";
    }

    //리뷰 작성 시 카테고리 추천
    @GetMapping("/bookrecommendation")
    public String bookrecommendation(HttpSession session, Model model,
                                     @RequestParam(defaultValue = "1")int cpage,
                                     @RequestParam(defaultValue = "20")int pageBlock,
                                     @RequestParam(required = false, defaultValue = "latest") String sortOrder){
        log.info("책 추천.." );

        //사용자가 작성한 리뷰카테고리 리스트로 가져옴
        String id = (String) session.getAttribute("id");
        log.info("id:{}", id);

        // id로 사용자 정보 조회 후 like_categories 가져오기
        UserVO user = userService.findById(id);
        List<String> likeCategories = Arrays.asList(user.getLike_categories().split(",\\s*"));
        log.info("likeCategories: {}", likeCategories);
        model.addAttribute("likeCategories", likeCategories);

        log.info("review book recommended cpage:{}, pageBlock:{}", cpage, pageBlock);

        String filter = "recommend";
        model.addAttribute("filter", filter);

        //사용자가 작성한 리뷰카테고리 리스트로 가져옴
        List<BookVO> list = service.getBookRecommendationBycategory(id, cpage, pageBlock, sortOrder);
        log.info("list:{}", list);

        int total_Row = service.bookGetRecommandationTotalRow(id);
        int totalPageCount= (int) Math.ceil((double) total_Row / pageBlock);
        log.info("total_Row:{}, totalPageCount:{}", total_Row, totalPageCount);

        model.addAttribute("totalPageCount", totalPageCount);
        model.addAttribute("list", list);
        model.addAttribute("cpage", cpage);
        model.addAttribute("pageBlock", pageBlock);
        model.addAttribute("sortOrder", sortOrder);

        return "book/list";

    }

}
