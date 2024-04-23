package actions;

import constants.MessageConst;
import constants.PropertyConst;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import actions.views.EmployeeView;
import constants.AttributeConst;
import constants.ForwardConst;
import constants.JpaConst;
import services.EmployeeService;

/**
 * 従業員に関わる処理を行うActionクラス
 *
 */
public class EmployeeAction extends ActionBase {

    private EmployeeService service;

    /**
     * メソッドを実行する
     */
    @Override
    public void process() throws ServletException, IOException {

        service = new EmployeeService();

        //メソッドを実行
        invoke();

        service.close();
    }

    /**
     * 一覧画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void index() throws ServletException, IOException {

        //指定されたページ数の一覧画面に表示するデータを取得
        int page = getPage();
        List<EmployeeView> employees = service.getPerPage(page);

        //全ての従業員データの件数を取得
        long employeeCount = service.countAll();

        putRequestScope(AttributeConst.EMPLOYEES, employees); //取得した従業員データ
        putRequestScope(AttributeConst.EMP_COUNT, employeeCount); //全ての従業員データの件数
        putRequestScope(AttributeConst.PAGE, page); //ページ数
        putRequestScope(AttributeConst.MAX_ROW, JpaConst.ROW_PER_PAGE); //1ページに表示するレコードの数

        //セッションにフラッシュメッセージが設定されている場合はリクエストスコープに移し替え、セッションからは削除する
        String flush = getSessionScope(AttributeConst.FLUSH);
        if (flush != null) {
            putRequestScope(AttributeConst.FLUSH, flush);
            removeSessionScope(AttributeConst.FLUSH);
        }

        //一覧画面を表示
        forward(ForwardConst.FW_EMP_INDEX);

    }

}

/**
 * 新規登録画面を表示する
 * @throws ServletException
 * @throws IOException
 */

/**
 * 新規登録を行う
 * @throws ServletException
 * @throws IOException
 */
public void create() throws ServletException, IOException {

    //CSRF対策 tokenのチェック
    if (checkToken()) {

        //パラメータの値を元に従業員情報のインスタンスを作成する
        EmployeeView ev = new EmployeeView(
                null,
                getRequestParam(AttributeConst.EMP_CODE),
                getRequestParam(AttributeConst.EMP_NAME),
                getRequestParam(AttributeConst.EMP_PASS),
                toNumber(getRequestParam(AttributeConst.EMP_ADMIN_FLG)),
                null,
                null,
                AttributeConst.DEL_FLAG_FALSE.getIntegerValue());

        //アプリケーションスコープからpepper文字列を取得
        String pepper = getContextScope(PropertyConst.PEPPER);

        //従業員情報登録
        List<String> errors = service.create(ev, pepper);

        if (errors.size() > 0) {
            //登録中にエラーがあった場合

            putRequestScope(AttributeConst.TOKEN, getTokenId()); //CSRF対策用トークン
            putRequestScope(AttributeConst.EMPLOYEE, ev); //入力された従業員情報
            putRequestScope(AttributeConst.ERR, errors); //エラーのリスト

            //新規登録画面を再表示
            forward(ForwardConst.FW_EMP_NEW);

        } else {
            //登録中にエラーがなかった場合

            //セッションに登録完了のフラッシュメッセージを設定
            putSessionScope(AttributeConst.FLUSH, MessageConst.I_REGISTERED.getMessage());

            //一覧画面にリダイレクト
            redirect(ForwardConst.ACT_EMP, ForwardConst.CMD_INDEX);
        }

    }
}

public void entryNew() throws ServletException, IOException {

    putRequestScope(AttributeConst.TOKEN, getTokenId()); //CSRF対策用トークン
    putRequestScope(AttributeConst.EMPLOYEE, new EmployeeView()); //空の従業員インスタンス

    //新規登録画面を表示
    forward(ForwardConst.FW_EMP_NEW);
}

/**
 * 詳細画面を表示する
 * @throws ServletException
 * @throws IOException
 */
public void show() throws ServletException, IOException {

    //idを条件に従業員データを取得する
    EmployeeView ev = service.findOne(toNumber(getRequestParam(AttributeConst.EMP_ID)));

    if (ev == null || ev.getDeleteFlag() == AttributeConst.DEL_FLAG_TRUE.getIntegerValue()) {

        //データが取得できなかった、または論理削除されている場合はエラー画面を表示
        forward(ForwardConst.FW_ERR_UNKNOWN);
        return;
    }

    putRequestScope(AttributeConst.EMPLOYEE, ev); //取得した従業員情報

    //詳細画面を表示
    forward(ForwardConst.FW_EMP_SHOW);
}
ビュー
/src/main/webapp/WEB-INF/views/employees フォルダーに show.jsp を作成します
WEB-INF/views/employees/show.jsp（/src/main/webapp/WEB-INF/views/employees/show.jsp）
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page import="constants.ForwardConst" %>
<%@ page import="constants.AttributeConst" %>

<c:set var="actEmp" value="${ForwardConst.ACT_EMP.getValue()}" />
<c:set var="commIdx" value="${ForwardConst.CMD_INDEX.getValue()}" />
<c:set var="commEdit" value="${ForwardConst.CMD_EDIT.getValue()}" />

<c:import url="/WEB-INF/views/layout/app.jsp">
    <c:param name="content">

        <h2>id : ${employee.id} の従業員情報 詳細ページ</h2>

        <table>
            <tbody>
                <tr>
                    <th>社員番号</th>
                    <td><c:out value="${employee.code}" /></td>
                </tr>
                <tr>
                    <th>氏名</th>
                    <td><c:out value="${employee.name}" /></td>
                </tr>
                <tr>
                    <th>権限</th>
                    <td><c:choose>
                            <c:when test="${employee.adminFlag == AttributeConst.ROLE_ADMIN.getIntegerValue()}">管理者</c:when>
                            <c:otherwise>一般</c:otherwise>
                        </c:choose></td>
                </tr>
                <tr>
                    <th>登録日時</th>
                    <fmt:parseDate value="${employee.createdAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="createDay" type="date" />
                    <td><fmt:formatDate value="${createDay}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
                </tr>
                <tr>
                    <th>更新日時</th>
                    <fmt:parseDate value="${employee.updatedAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="updateDay" type="date" />
                    <td><fmt:formatDate value="${updateDay}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
                </tr>
            </tbody>
        </table>

        <p>
            <a href="<c:url value='?action=${actEmp}&command=${commEdit}&id=${employee.id}' />">この従業員情報を編集する</a>
        </p>

        <p>
            <a href="<c:url value='?action=${actEmp}&command=${commIdx}' />">一覧に戻る</a>
        </p>
    </c:param>
</c:import>

/**
 * 編集画面を表示する
 * @throws ServletException
 * @throws IOException
 */
public void edit() throws ServletException, IOException {

    //idを条件に従業員データを取得する
    EmployeeView ev = service.findOne(toNumber(getRequestParam(AttributeConst.EMP_ID)));

    if (ev == null || ev.getDeleteFlag() == AttributeConst.DEL_FLAG_TRUE.getIntegerValue()) {

        //データが取得できなかった、または論理削除されている場合はエラー画面を表示
        forward(ForwardConst.FW_ERR_UNKNOWN);
        return;
    }

    putRequestScope(AttributeConst.TOKEN, getTokenId()); //CSRF対策用トークン
    putRequestScope(AttributeConst.EMPLOYEE, ev); //取得した従業員情報

    //編集画面を表示する
    forward(ForwardConst.FW_EMP_EDIT);

}

/**
 * 更新を行う
 * @throws ServletException
 * @throws IOException
 */
public void update() throws ServletException, IOException {

    //CSRF対策 tokenのチェック
    if (checkToken()) {
        //パラメータの値を元に従業員情報のインスタンスを作成する
        EmployeeView ev = new EmployeeView(
                toNumber(getRequestParam(AttributeConst.EMP_ID)),
                getRequestParam(AttributeConst.EMP_CODE),
                getRequestParam(AttributeConst.EMP_NAME),
                getRequestParam(AttributeConst.EMP_PASS),
                toNumber(getRequestParam(AttributeConst.EMP_ADMIN_FLG)),
                null,
                null,
                AttributeConst.DEL_FLAG_FALSE.getIntegerValue());

        //アプリケーションスコープからpepper文字列を取得
        String pepper = getContextScope(PropertyConst.PEPPER);

        //従業員情報更新
        List<String> errors = service.update(ev, pepper);

        if (errors.size() > 0) {
            //更新中にエラーが発生した場合

            putRequestScope(AttributeConst.TOKEN, getTokenId()); //CSRF対策用トークン
            putRequestScope(AttributeConst.EMPLOYEE, ev); //入力された従業員情報
            putRequestScope(AttributeConst.ERR, errors); //エラーのリスト

            //編集画面を再表示
            forward(ForwardConst.FW_EMP_EDIT);
        } else {
            //更新中にエラーがなかった場合

            //セッションに更新完了のフラッシュメッセージを設定
            putSessionScope(AttributeConst.FLUSH, MessageConst.I_UPDATED.getMessage());

            //一覧画面にリダイレクト
            redirect(ForwardConst.ACT_EMP, ForwardConst.CMD_INDEX);
        }
    }
}
/**
 * 論理削除を行う
 * @throws ServletException
 * @throws IOException
 */
public void destroy() throws ServletException, IOException {

    //CSRF対策 tokenのチェック
    if (checkToken()) {

        //idを条件に従業員データを論理削除する
        service.destroy(toNumber(getRequestParam(AttributeConst.EMP_ID)));

        //セッションに削除完了のフラッシュメッセージを設定
        putSessionScope(AttributeConst.FLUSH, MessageConst.I_DELETED.getMessage());

        //一覧画面にリダイレクト
        redirect(ForwardConst.ACT_EMP, ForwardConst.CMD_INDEX);
    }
}

/**
 * ログイン中の従業員が管理者かどうかチェックし、管理者でなければエラー画面を表示
 * true: 管理者 false: 管理者ではない
 * @throws ServletException
 * @throws IOException
 */
private boolean checkAdmin() throws ServletException, IOException {

    //セッションからログイン中の従業員情報を取得
    EmployeeView ev = (EmployeeView) getSessionScope(AttributeConst.LOGIN_EMP);

    //管理者でなければエラー画面を表示
    if (ev.getAdminFlag() != AttributeConst.ROLE_ADMIN.getIntegerValue()) {

        forward(ForwardConst.FW_ERR_UNKNOWN);
        return false;

    } else {

        return true;
    }

}
