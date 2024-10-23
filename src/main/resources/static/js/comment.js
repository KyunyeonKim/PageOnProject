
function fetchChildComments(commentId, page = 1) {
    const childCommentsDiv = $('#childComments-' + commentId);
    const pagingDiv = $('#paging-' + commentId);

    // AJAX 요청
    $.ajax({
        type: "GET",
        url: "/comments/child/" + commentId + "?cpage=" + page,
        success: function(response) {
            const childComments = response.childComments;
            const totalPageCount = response.totalPageCount;

            // 대댓글을 추가
            childCommentsDiv.empty();
            if (childComments.length > 0) {
                childComments.forEach(function(childComment) {
                    const childCommentElem = `
                <div class="childComment" id="childComment-${childComment.num}">
                    <p><strong>${childComment.user_id}:</strong> ${childComment.content}</p>
                    <p>작성일: ${childComment.wdate}</p>
                    <p>
                        <img src="/img/comment_child.png" alt="대댓글 더보기" onclick="fetchChildComments(${childComment.num})" class="comment-icon"/>
                        <img src="/img/comment_childup.png" alt="대댓글 줄이기" onclick="hideComment(${childComment.num})" class="comment-icon"/>
                        <img src="/img/comment_write.png" alt="대댓글 작성" onclick="showReplyForm(${childComment.num}, true)" class="comment-icon"/>
                        <img src="/img/comment_update.png" alt="수정" onclick="editComment(${childComment.num}, true)" class="comment-icon"/>
                        <img src="/img/comment_delete.png" alt="삭제" onclick="deleteComment(${childComment.num}, true)" class="comment-icon"/>
                        <img src="/img/comment_report.png" alt="신고" onclick="reportComment(${childComment.num}, true)" class="comment-icon"/>
                    </p>

                    <div class="replyForm" id="childReplyForm-${childComment.num}" style="display:none;">
                        <textarea id="replyText-${childComment.num}" rows="2" placeholder="대댓글을 입력하세요..."></textarea>
                        <img src="/img/comment_write.png" alt="작성" onclick="submitReply(${childComment.num}, true)" class="comment-icon"/>
                        <img src="/img/comment_delete.png" alt="취소" onclick="cancelReply(${childComment.num}, true)" class="comment-icon"/>
                    </div>

                    <div class="editForm" id="childEditForm-${childComment.num}" style="display:none;">
                        <textarea id="editText-${childComment.num}" rows="2" text="${childComment.content}"></textarea>
                        <img src="/img/comment_update.png" alt="수정" onclick="submitEdit(${childComment.num}, true)" class="comment-icon"/>
                        <img src="/img/comment_delete.png" alt="취소" onclick="cancelEdit(${childComment.num}, true)" class="comment-icon"/>
                    </div>

                    <div class="childComments" id="childComments-${childComment.num}">
                        <!-- 대댓글의 대댓글을 가져올 공간 -->
                    </div>

                    <div id="paging-${childComment.num}" style="display:none;">
                        <!-- 페이징을 위한 div 추가 -->
                    </div>
                </div>
                `;
                    childCommentsDiv.append(childCommentElem);
                });
            }

            // 페이징 정보 추가
            updatePaging(pagingDiv, totalPageCount, page, commentId);
            childCommentsDiv.show();
            pagingDiv.show();
        },
        error: function() {
            alert("대댓글을 가져오는 데 오류가 발생했습니다.");
        }
    });
}

// 페이징 버튼 생성 함수
function updatePaging(pagingDiv, totalPageCount, currentPage, commentId) {
    pagingDiv.empty(); // 기존 페이징 내용을 지우기

    // 페이지 수가 1 이하일 경우 페이징 div를 숨김
    if (totalPageCount <= 1) {
        pagingDiv.hide();
        return;
    }

    // 페이지 링크 생성
    for (let i = 1; i <= totalPageCount; i++) {
        const pageLink = $(`<span class="page-link" onclick="fetchChildComments(${commentId}, ${i})">${i}</span>`);
        if (i === currentPage) {
            pageLink.addClass('active'); // 현재 페이지 강조
        }
        pagingDiv.append(pageLink);
    }

    // 페이징 div를 보여줌
    pagingDiv.show();
}

function hideComment(commentId) {
    const childCommentsDiv = $('#childComments-' + commentId); // 대댓글 div
    const toggleButton = childCommentsDiv.siblings('img[alt="대댓글 줄이기"]'); // 줄이기 버튼
    const showMoreButton = childCommentsDiv.siblings('img[alt="대댓글 더보기"]'); // 더보기 버튼
    const pagingDiv = $('#paging-' + commentId); // 페이징 div

    // 대댓글 목록 숨기기
    childCommentsDiv.hide(); // 대댓글 목록 숨기기
    toggleButton.hide(); // 줄이기 버튼 숨기기
    showMoreButton.show(); // 더보기 버튼 보이기
    pagingDiv.hide(); // 페이징 숨기기
}

function submitComment() {
    const commentText = $('#commentText').val().trim();
    const bnum = $('#bnum').val(); // 게시글 번호 추가
    const type = $('#type').val(); // 게시글 타입 추가
    if (!commentText) {
        alert("댓글 내용을 입력하세요.");
        return;
    }

    $.ajax({
        type: "POST",
        url: "/comments",
        contentType: "application/json",
        data: JSON.stringify({
            content: commentText,
            type: type, // 부모글 타입 (type 추가)
            bnum: bnum, // 부모글id (fnum -> bnum 변경)
            user_id: "user123" // 로그인id
        }),
        success: function() {
            location.reload();
        },
        error: function() {
            alert("댓글 작성에 실패했습니다.");
        }
    });
}

function submitReply(parentId) {
    const replyText = $(`#replyText-${parentId}`).val().trim();
    if (!replyText) {
        alert("대댓글 내용을 입력하세요.");
        return;
    }

    $.ajax({
        type: "POST",
        url: "/comments",
        contentType: "application/json",
        data: JSON.stringify({
            content: replyText,
            type: "forum", // 부모글 타입
            fnum: 1, // 부모글id
            cnum: parentId,
            user_id: "user123" // 로그인id
        }),
        success: function() {
            location.reload();
        },
        error: function() {
            alert("대댓글 작성에 실패했습니다.");
        }
    });
}

function showReplyForm(commentId, isChild = false) {
    // 대댓글의 ID에 맞춰 폼 ID를 다르게 설정
    const formId = isChild ? 'childReplyForm-' + commentId : 'replyForm-' + commentId;
    document.getElementById(formId).style.display = 'block'; // 해당 폼을 보이게 함
}

function cancelReply(commentId, isChild = false) {
    const formId = isChild ? 'childReplyForm-' + commentId : 'replyForm-' + commentId;
    document.getElementById(formId).style.display = 'none'; // 해당 폼 숨기기
}

function editComment(commentId, isChild = false) {
    // 대댓글의 ID에 맞춰 수정 폼 ID 설정
    const editFormId = isChild ? `#childEditForm-${commentId}` : `#editForm-${commentId}`;
    const commentElemId = isChild ? `#childComment-${commentId}` : `#comment-${commentId}`;

    // 대댓글 또는 일반 댓글에서 콘텐츠를 가져옴
    const commentContentElem = $(commentElemId).find('p').first();

    // 작성자의 이름과 내용을 분리하여 내용만 가져옴
    const fullCommentText = commentContentElem.length > 0 ? commentContentElem.text().trim() : '';

    // 작성자 이름과 내용 분리 (작성자: 내용 형태)
    const contentStartIndex = fullCommentText.indexOf(':') + 2; // ': ' 다음 인덱스부터 시작
    const commentText = fullCommentText.substring(contentStartIndex).trim(); // 내용만 추출

    // 기존 내용으로 텍스트 에어리어를 업데이트
    $(editFormId).find('textarea').val(commentText);
    $(editFormId).show(); // 수정 폼을 보이게 함
}

function submitEdit(commentId) {
    const newCommentText = $(`#editText-${commentId}`).val().trim();

    $.ajax({
        type: "PUT",
        url: "/comments/" + commentId,
        contentType: "application/json",
        data: JSON.stringify({
            content: newCommentText,
            user_id: "user123" // 로그인id
        }),
        success: function() {
            location.reload();
        },
        error: function() {
            alert("댓글 수정에 실패했습니다.");
        }
    });
}

function cancelEdit(commentId, isChild = false) {
    const editFormId = isChild ? '#childEditForm-' + commentId : '#editForm-' + commentId;
    $(editFormId).hide(); // 해당 수정 폼 숨기기
}

function deleteComment(commentId, isChild = false) {
    if (confirm("정말로 이 댓글을 삭제하시겠습니까?")) {
        $.ajax({
            type: "DELETE",
            url: "/comments/" + commentId,
            success: function() {
                const commentSelector = isChild ? `#childComment-${commentId}` : `#comment-${commentId}`;
                $(commentSelector).remove();
            },
            error: function() {
                alert("댓글 삭제에 실패했습니다.");
            }
        });
    }
}

function reportComment(commentId) {
    // 신고 요청을 보내는 AJAX 호출
    $.ajax({
        type: "PUT",
        url: "/comments/report/" + commentId, // 신고를 처리할 경로
        success: function() {
            alert("댓글이 신고되었습니다.");
        },
        error: function() {
            alert("댓글 신고에 실패했습니다.");
        }
    });
}