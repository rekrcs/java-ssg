//화이팅
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class Main {

	public static void main(String[] args) {
		App app = new App();

		app.start();
	}
}

// Session
// 현재 사용자가 이용중인 정보
// 이 안의 정보는 사용자가 프로그램을 사용할 때 동안은 계속 유지된다.
class Session {
	private Member loginedMember;
	private Board currentBoard;

	public Member getLoginedMember() {
		return loginedMember;
	}

	public void setLoginedMember(Member loginedMember) {
		this.loginedMember = loginedMember;
	}

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void setCurrentBoard(Board currentBoard) {
		this.currentBoard = currentBoard;
	}

	public boolean isLogined() {
		return loginedMember != null;
	}
}

// Factory
// 프로그램 전체에서 공유되는 객체 리모콘을 보관하는 클래스

class Factory {
	private static Session session;
	private static DB db;
	private static BuildService buildService;
	private static ArticleService articleService;
	private static ArticleDao articleDao;
	private static MemberService memberService;
	private static MemberDao memberDao;
	private static Scanner scanner;
	private static Stat stat;

	public static Stat getStat() {
		if (stat == null) {
			stat = new Stat();
		}
		return stat;
	}

	public static Session getSession() {
		if (session == null) {
			session = new Session();
		}

		return session;
	}

	public static Scanner getScanner() {
		if (scanner == null) {
			scanner = new Scanner(System.in);
		}

		return scanner;
	}

	public static DB getDB() {
		if (db == null) {
			db = new DB();
		}

		return db;
	}

	public static ArticleService getArticleService() {
		if (articleService == null) {
			articleService = new ArticleService();
		}

		return articleService;
	}

	public static ArticleDao getArticleDao() {
		if (articleDao == null) {
			articleDao = new ArticleDao();
		}

		return articleDao;
	}

	public static MemberService getMemberService() {
		if (memberService == null) {
			memberService = new MemberService();
		}
		return memberService;
	}

	public static MemberDao getMemberDao() {
		if (memberDao == null) {
			memberDao = new MemberDao();
		}

		return memberDao;
	}

	public static BuildService getBuildService() {
		if (buildService == null) {
			buildService = new BuildService();
		}

		return buildService;
	}
}

// App
class App {
	private Map<String, Controller> controllers;

	// 컨트롤러 만들고 한곳에 정리
	// 나중에 컨트롤러 이름으로 쉽게 찾아쓸 수 있게 하려고 Map 사용
	void initControllers() {
		controllers = new HashMap<>();
		controllers.put("build", new BuildController());
		controllers.put("article", new ArticleController());
		controllers.put("member", new MemberController());
	}

	public App() {
		// 컨트롤러 등록
		initControllers();

		// 관리자 회원 생성
		Factory.getMemberService().join("admin", "admin", "관리자");

		// 공지사항 게시판 생성
		Factory.getArticleService().makeBoard("공지시항", "notice");
		// 자유 게시판 생성
		Factory.getArticleService().makeBoard("자유게시판", "free");

		// 현재 게시판을 1번 게시판으로 선택
		Factory.getSession().setCurrentBoard(Factory.getArticleService().getBoard(1));
		// 임시 : 현재 로그인 된 회원은 1번 회원으로 지정, 이건 나중에 회원가입, 로그인 추가되면 제거해야함
		Factory.getSession().setLoginedMember(Factory.getMemberService().getMember(1));
	}

	public void start() {

		while (true) {
			System.out.printf("명령어 : ");
			String command = Factory.getScanner().nextLine().trim();

			if (command.length() == 0) {
				continue;
			} else if (command.equals("exit")) {
				break;
			}

			Request reqeust = new Request(command);

			if (reqeust.isValidRequest() == false) {
				continue;
			}

			if (controllers.containsKey(reqeust.getControllerName()) == false) {
				continue;
			}

			controllers.get(reqeust.getControllerName()).doAction(reqeust);
		}

		Factory.getScanner().close();
	}
}

// Request
class Request {
	private String requestStr;
	private String controllerName;
	private String actionName;
	private String arg1;
	private String arg2;
	private String arg3;

	boolean isValidRequest() {
		return actionName != null;
	}

	Request(String requestStr) {
		this.requestStr = requestStr;
		String[] requestStrBits = requestStr.split(" ");
		this.controllerName = requestStrBits[0];

		if (requestStrBits.length > 1) {
			this.actionName = requestStrBits[1];
		}

		if (requestStrBits.length > 2) {
			this.arg1 = requestStrBits[2];
		}

		if (requestStrBits.length > 3) {
			this.arg2 = requestStrBits[3];
		}

		if (requestStrBits.length > 4) {
			this.arg3 = requestStrBits[4];
		}
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}

	public String getArg3() {
		return arg3;
	}

	public void setArg3(String arg3) {
		this.arg3 = arg3;
	}
}

// Controller
abstract class Controller {
	abstract void doAction(Request reqeust);
}

class ArticleController extends Controller {
	private ArticleService articleService;

	ArticleController() {
		articleService = Factory.getArticleService();
	}

	public void doAction(Request request) {
		if (request.getActionName().equals("list")) {
			actionList(request);
		} else if (request.getActionName().equals("write")) {
			actionWrite(request);
		} else if (request.getActionName().equals("modify")) {
			actionModify(request);
		} else if (request.getActionName().equals("delete")) {
			actionDelete(request);
		} else if (request.getActionName().equals("detail")) {
			actionDetail(request);
		} else if (request.getActionName().equals("createBoard")) {
			actionCreateBoard(request);
		} else if (request.getActionName().equals("deleteBoard")) {
			actionDeleteBoard(request);
		} else if (request.getActionName().equals("listBoard")) {
			actionListBoard(request);
		} else if (request.getActionName().equals("changeBoard")) {
			actionChangeBoard(request);
		}
	}

	private void actionChangeBoard(Request request) {
		String code = request.getArg1();
		request.setArg1(code);

		Factory.getSession().setCurrentBoard(Factory.getArticleDao().getBoardByCode(code));
		System.out.println(Factory.getArticleDao().getBoardByCode(code).getName() + "으로 게시판이 변경 되었습니다.");
	}

	private void actionListBoard(Request request) {
		List<Board> boards = Factory.getArticleService().getBoards();
		String filePath = "db/article/boardList.json";
		Util.writeJsonFile(filePath, boards);
		System.out.println(boards);
	}

	private void actionDeleteBoard(Request request) {
		String code = request.getArg1();
		request.setArg1(code);
		Board board = Factory.getArticleDao().getBoardByCode(code);

		int deleteNum = board.getId();
		String filePath = "db/board/" + deleteNum + ".json";

		File deleteFile = new File(filePath);
		if (deleteFile.exists()) {

			deleteFile.delete();

			System.out.println("게시판을 삭제하였습니다.");

		} else {
			System.out.println("게시판이 존재하지 않습니다.");
		}

		// 게시판별 게시물 총갯수
//		List<Article> list = Factory.getArticleService().getArticlesByBoardCode(code);
//		System.out.println(list);
	}

	private void actionCreateBoard(Request request) {
		System.out.print("새 게시판 이름: ");
		String name = Factory.getScanner().nextLine();

		System.out.print("새 게시판 코드: ");
		String code = Factory.getScanner().nextLine();
		Factory.getArticleService().makeBoard(name, code);
		System.out.println("새 게시판 " + name + "이(가) 생성되었습니다.");

	}

	private void actionDetail(Request request) {
		String str = request.getArg1();
		request.setArg1(str);
		int detailNum = Integer.parseInt(str);

		Article article = Factory.getArticleService().getArticle(detailNum);
		int views = article.getViews();
		views += 1;
		article.setViews(views);
		String filePath = "db/article/" + detailNum + ".json";
		Util.writeJsonFile(filePath, article);
		System.out.println(article);

	}

	private void actionDelete(Request request) {
		String str = request.getArg1();
		request.setArg1(str);
		int deleteNum = Integer.parseInt(str);
		String filePath = "db/article/" + deleteNum + ".json";

		File deleteFile = new File(filePath);
		if (deleteFile.exists()) {

			deleteFile.delete();

			System.out.println("게시물을 삭제하였습니다.");

		} else {
			System.out.println("게시물이 존재하지 않습니다.");
		}

		String filePathHtml = "site/article/" + deleteNum + ".html";
		File deleteFile1 = new File(filePathHtml);
		if (deleteFile1.exists()) {
			deleteFile1.delete();
		}
	}

	private void actionModify(Request request) {
		String str = request.getArg1();
		request.setArg1(str);
		int modiftyNum = Integer.parseInt(str);

		Article article = Factory.getArticleService().getArticle(modiftyNum);
		System.out.printf("제목 : ");
		String newTitle = Factory.getScanner().nextLine();
		article.setTitle(newTitle);
		System.out.printf("내용 : ");
		String newBody = Factory.getScanner().nextLine();
		article.setBody(newBody);

		String filePath = "db/article/" + article.getId() + ".json";
		Util.writeJsonFile(filePath, article);

		System.out.println(article);

	}

	private void actionList(Request reqeust) {
		List<Article> articles = articleService.getArticles();
		String filePath = "db/article/list.json";
		Util.writeJsonFile(filePath, articles);
		System.out.println(articles);
	}

	private void actionWrite(Request reqeust) {
		System.out.printf("제목 : ");
		String title = Factory.getScanner().nextLine();
		System.out.printf("내용 : ");
		String body = Factory.getScanner().nextLine();
		int views = 0;
		// 현재 게시판 id 가져오기
		int boardId = Factory.getSession().getCurrentBoard().getId();

		// 현재 로그인한 회원의 id 가져오기
		int memberId = Factory.getSession().getLoginedMember().getId();
		int newId = articleService.write(boardId, memberId, title, body, views);

		System.out.printf("%d번 글이 생성되었습니다.\n", newId);
	}
}

class BuildController extends Controller {
	private BuildService buildService;

	BuildController() {
		buildService = Factory.getBuildService();
	}

	@Override
	void doAction(Request request) {
		if (request.getActionName().equals("site")) {
			actionSite(request);
		} else if (request.getActionName().equals("startAutoSite")) {
			actionStartAutoSite(request);
		} else if (request.getActionName().equals("stopAutoSite")) {
			actionStopAutoSite(request);
		}
	}

	private void actionStopAutoSite(Request request) {
		System.out.println("자동실행 종료");
		Factory.getBuildService().workStarted = false;

	}

	private void actionStartAutoSite(Request request) {
		Factory.getBuildService().startWork();

	}

	private void actionSite(Request reqeust) {
		buildService.buildSite();
	}
}

class MemberController extends Controller {
	private MemberService memberService;

	MemberController() {
		memberService = Factory.getMemberService();
	}

	void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("logout")) {
			actionLogout(reqeust);
		} else if (reqeust.getActionName().equals("login")) {
			actionLogin(reqeust);
		} else if (reqeust.getActionName().equals("whoami")) {
			actionWhoami(reqeust);
		} else if (reqeust.getActionName().equals("join")) {
			actionJoin(reqeust);
		}
	}

	private void actionJoin(Request reqeust) {
		System.out.print("로그인 아이디 : ");
		String loginId = Factory.getScanner().nextLine();
		System.out.print("로그인 비번 : ");
		String loginPw = Factory.getScanner().nextLine();
		System.out.print("이름 : ");
		String name = Factory.getScanner().nextLine();
		int newId = memberService.join(loginId, loginPw, name);
	}

	private void actionWhoami(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember == null) {
			System.out.println("로그인 된 아이디가 없습니다.");
		} else {
			System.out.println(loginedMember.getName());
		}

	}

	private void actionLogin(Request reqeust) {
		System.out.printf("로그인 아이디 : ");
		String loginId = Factory.getScanner().nextLine().trim();

		System.out.printf("로그인 비번 : ");
		String loginPw = Factory.getScanner().nextLine().trim();

		Member member = memberService.getMemberByLoginIdAndLoginPw(loginId, loginPw);

		if (member == null) {
			System.out.println("일치하는 회원이 없습니다.");
		} else {
			System.out.println(member.getName() + "님 환영합니다.");
			Factory.getSession().setLoginedMember(member);
		}
	}

	private void actionLogout(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			Session session = Factory.getSession();
			System.out.println("로그아웃 되었습니다.");
			session.setLoginedMember(null);
		}

	}
}

// Service
class BuildService {
	ArticleService articleService;
	static boolean workStarted;

	static {
		workStarted = false;
	}

	BuildService() {
		articleService = Factory.getArticleService();
	}

	static void startWork() {
		workStarted = true;
		new Thread(() -> {
			while (workStarted) {
				Factory.getBuildService().buildSite();
//				System.out.println(Thread.currentThread() + " : 자동실행중");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
			}
		}).start();
	}

	public void buildSite() {
		Util.makeDir("site");
		Util.makeDir("site/article");
		Util.makeDir("site/home");
		Util.makeDir("site/stat");

		String html;

		String head = Util.getFileContents("site_template/part/head.html");
		String foot = Util.getFileContents("site_template/part/foot.html");

		// 홈(index.html)
		html = "";

		html += "<div>" + "</div>";
		html = head + html + foot;
		Util.writeFileContents("site/home/" + "index.html", html);

		// 통계(index.html)
		html = "";
		html += "<div>회원수: " + Factory.getStat().totalMember() + "</div>";
		html += "<div>총 게시물수: " + Factory.getStat().totalArticleNumber() + "</div>";
		html += "<div>총 조회수: " + Factory.getStat().totalArticleView() + "</div>";

		List<Board> boards = Factory.getArticleService().getBoards();
		for (Board board : boards) {
			List<Article> articles = Factory.getArticleService().getArticlesByBoardCode(board.getCode());
			int total = 0;
			for (Article article : articles) {
				total += article.getViews();
			}
			html = head + html + foot;
			html += "<div>" + board.getName() + " 조회수: " + total + "</div>";
		}

		List<Board> boards6 = Factory.getArticleService().getBoards();
		for (Board board1 : boards6) {
			List<Article> articles = Factory.getArticleService().getArticlesByBoardCode(board1.getCode());
			int numberOfArticleInEachBoard = articles.size();
			html += "<div>" + board1.getName() + " 게시물 수 : " + numberOfArticleInEachBoard + "</div>";
		}
		Util.writeFileContents("site/stat/" + "index.html", html);

		// 전체 게시물(list.html)
		
		html = "";
		List<Article> articles1 = articleService.getArticles();
		String template = Util.getFileContents("site_template/article/test.html");
		for (Article article : articles1) {
			html += "<div>";
			html += "<div>번호: " + article.getId() + "</div>";
			html += "<div>날짜: " + article.getRegDate() + "</div>";
			html += "<div>제목: " + article.getTitle() + "</div>";
			html += "<div>내용: " + article.getBody() + "</div>";
			html += "<div>조회수: " + article.getViews() + "</div>";
			
			html = template.replace("${TR}", html);
			
			html = head + html + foot;

			Util.writeFileContents("site/article/" + "list.html", html);
		}

		// 게시판 리스트(boardList.html)
		List<Board> boards1 = Factory.getArticleService().getBoards();
		html = "";
		for (Board board : boards1) {
			html += "<div>";
			html += "<div>게시판 번호: " + board.getId() + "</div>";
			html += "<div>날짜: " + board.getRegDate() + "</div>";
			html += "<div>게시판 이름: " + board.getName() + "</div>";
			html += "<div>게시판 코드: " + board.getCode() + "</div>";

			html = head + html + foot;

			Util.writeFileContents("site/article/" + "boardList.html", html);
		}
		// 게시판별 게시물 리스트
		List<Board> boards5 = articleService.getBoards();

		for (Board board : boards5) {
			String fileName = board.getCode() + "-list-1.html";

			html = "";

			List<Article> articles = articleService.getArticlesByBoardCode(board.getCode());

			for (Article article : articles) {
				html += "<div>";
				html += "<div>번호: " + article.getId() + "</div>";
				html += "<div>날짜: " + article.getRegDate() + "</div>";
				html += "<div>제목: <a href=\"" + article.getId() + ".html\">" + article.getTitle() + "</a></div>";
				html += "</tr>";
			}

			html = head + html + foot;

			Util.writeFileContents("site/article/" + fileName, html);
		}

		// 게시물별 파일생성(#.html)
		List<Article> articles = articleService.getArticles();

		for (Article article : articles) {
			html = "";

			html += "<div>제목 : " + article.getTitle() + "</div>";
			html += "<div>내용 : " + article.getBody() + "</div>";
			html += "<div><a href=\"" + (article.getId() - 1) + ".html\">이전글</a></div>";
			html += "<div><a href=\"" + (article.getId() + 1) + ".html\">다음글</a></div>";

			html = head + html + foot;

			Util.writeFileContents("site/article/" + article.getId() + ".html", html);
		}
	}

}

class ArticleService {
	private ArticleDao articleDao;

	ArticleService() {
		articleDao = Factory.getArticleDao();
	}

	public Article getArticle(int id) {
		return articleDao.getArticle(id);

	}

	public List<Article> getArticlesByBoardCode(String code) {
		return articleDao.getArticlesByBoardCode(code);
	}

	public List<Board> getBoards() {
		return articleDao.getBoards();
	}

	public int makeBoard(String name, String code) {
		Board oldBoard = articleDao.getBoardByCode(code);

		if (oldBoard != null) {
			return -1;
		}

		Board board = new Board(name, code);
		return articleDao.saveBoard(board);
	}

	public Board getBoard(int id) {
		return articleDao.getBoard(id);
	}

	public int write(int boardId, int memberId, String title, String body, int views) {
		Article article = new Article(boardId, memberId, title, body, views);

		return articleDao.save(article);
	}

	public List<Article> getArticles() {
		return articleDao.getArticles();
	}

}

class MemberService {
	private MemberDao memberDao;

	MemberService() {
		memberDao = Factory.getMemberDao();
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		return memberDao.getMemberByLoginIdAndLoginPw(loginId, loginPw);
	}

	public int join(String loginId, String loginPw, String name) {

		Member oldMember = memberDao.getMemberByLoginId(loginId);

		if (oldMember != null) {
			return -1;
		}

		Member member = new Member(loginId, loginPw, name);
		return memberDao.save(member);
	}

	public Member getMember(int id) {
		return memberDao.getMember(id);
	}
}

// Dao
class ArticleDao {
	DB db;

	ArticleDao() {
		db = Factory.getDB();
	}

	public Article getArticle(int id) {
		// TODO Auto-generated method stub
		return db.getArticle(id);
	}

	public void load(int modiftyNum) {
		db.loadArticle(modiftyNum);

	}

	public List<Article> getArticlesByBoardCode(String code) {
		return db.getArticlesByBoardCode(code);
	}

	public List<Board> getBoards() {
		return db.getBoards();
	}

	public Board getBoardByCode(String code) {
		return db.getBoardByCode(code);
	}

	public int saveBoard(Board board) {
		return db.saveBoard(board);
	}

	public int save(Article article) {
		return db.saveArticle(article);
	}

	public Board getBoard(int id) {
		return db.getBoard(id);
	}

	public List<Article> getArticles() {
		return db.getArticles();
	}

}

class MemberDao {
	DB db;

	MemberDao() {
		db = Factory.getDB();
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		return db.getMemberByLoginIdAndLoginPw(loginId, loginPw);
	}

	public Member getMemberByLoginId(String loginId) {
		return db.getMemberByLoginId(loginId);
	}

	public Member getMember(int id) {
		return db.getMember(id);
	}

	public int save(Member member) {
		return db.saveMember(member);
	}
}

// DB
class DB {
	private Map<String, Table> tables;

	public DB() {
		String dbDirPath = getDirPath();
		Util.makeDir(dbDirPath);

		tables = new HashMap<>();

		tables.put("article", new Table<Article>(Article.class, dbDirPath));
		tables.put("board", new Table<Board>(Board.class, dbDirPath));
		tables.put("member", new Table<Member>(Member.class, dbDirPath));
	}

	public void loadArticle(int modiftyNum) {
		// TODO Auto-generated method stub

	}

	public List<Article> getArticlesByBoardCode(String code) {
		Board board = getBoardByCode(code);
		// free => 2
		// notice => 1

		List<Article> articles = getArticles();
		List<Article> newArticles = new ArrayList<>();

		for (Article article : articles) {
			if (article.getBoardId() == board.getId()) {
				newArticles.add(article);
			}
		}

		return newArticles;
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId) && member.getLoginPw().equals(loginPw)) {
				return member;
			}
		}

		return null;
	}

	public Member getMemberByLoginId(String loginId) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId)) {
				return member;
			}
		}

		return null;
	}

	public List<Member> getMembers() {
		return tables.get("member").getRows();
	}

	public Board getBoardByCode(String code) {
		List<Board> boards = getBoards();

		for (Board board : boards) {
			if (board.getCode().equals(code)) {
				return board;
			}
		}

		return null;
	}

	public List<Board> getBoards() {
		return tables.get("board").getRows();
	}

	public Member getMember(int id) {
		return (Member) tables.get("member").getRow(id);
	}

	public Article getArticle(int id) {
		return (Article) tables.get("article").getRow(id);
	}

	public int saveBoard(Board board) {
		return tables.get("board").saveRow(board);
	}

	public String getDirPath() {
		return "db";
	}

	public int saveMember(Member member) {
		return tables.get("member").saveRow(member);
	}

	public Board getBoard(int id) {
		return (Board) tables.get("board").getRow(id);
	}

	public List<Article> getArticles() {
		return tables.get("article").getRows();
	}

	public int saveArticle(Article article) {
		return tables.get("article").saveRow(article);
	}

	public void backup() {
		for (String tableName : tables.keySet()) {
			Table table = tables.get(tableName);
			table.backup();
		}
	}
}

// Table
class Table<T> {
	private Class<T> dataCls;
	private String tableName;
	private String tableDirPath;

	public Table(Class<T> dataCls, String dbDirPath) {
		this.dataCls = dataCls;
		this.tableName = Util.lcfirst(dataCls.getCanonicalName());
		this.tableDirPath = dbDirPath + "/" + this.tableName;

		Util.makeDir(tableDirPath);
	}

	private String getTableName() {
		return tableName;
	}

	public int saveRow(T data) {
		Dto dto = (Dto) data;

		if (dto.getId() == 0) {
			int lastId = getLastId();
			int newId = lastId + 1;
			dto.setId(newId);
			setLastId(newId);
		}

		String rowFilePath = getRowFilePath(dto.getId());

		Util.writeJsonFile(rowFilePath, data);

		return dto.getId();
	};

	private String getRowFilePath(int id) {
		return tableDirPath + "/" + id + ".json";
	}

	private void setLastId(int lastId) {
		String filePath = getLastIdFilePath();
		Util.writeFileContents(filePath, lastId);
	}

	private int getLastId() {
		String filePath = getLastIdFilePath();

		if (Util.isFileExists(filePath) == false) {
			int lastId = 0;
			Util.writeFileContents(filePath, lastId);
			return lastId;
		}

		return Integer.parseInt(Util.getFileContents(filePath));
	}

	private String getLastIdFilePath() {
		return this.tableDirPath + "/lastId.txt";
	}

	public T getRow(int id) {
		return (T) Util.getObjectFromJson(getRowFilePath(id), dataCls);
	}

	public void backup() {

	}

	void delete(int id) {
		/* 구현 */
	};

	List<T> getRows() {
		int lastId = getLastId();

		List<T> rows = new ArrayList<>();

		for (int id = 1; id <= lastId; id++) {
			T row = getRow(id);

			if (row != null) {
				rows.add(row);
			}
		}

		return rows;
	};
}

// DTO
abstract class Dto {
	private int id;
	private String regDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRegDate() {
		return regDate;
	}

	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}

	Dto() {
		this(0);
	}

	Dto(int id) {
		this(id, Util.getNowDateStr());
	}

	Dto(int id, String regDate) {
		this.id = id;
		this.regDate = regDate;
	}
}

class Board extends Dto {
	private String name;
	private String code;

	public Board() {
	}

	public Board(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return "Board [name=" + name + ", code=" + code + ", getId()=" + getId() + ", getRegDate()=" + getRegDate()
				+ "]";
	}

}

class Article extends Dto {
	private int boardId;
	private int memberId;
	private String title;
	private String body;
	private int views;

	public Article() {

	}

	public Article(int boardId, int memberId, String title, String body, int views) {
		this.boardId = boardId;
		this.memberId = memberId;
		this.title = title;
		this.body = body;
		this.views = views;
	}

	public int getViews() {
		return views;
	}

	public void setViews(int views) {
		this.views = views;
	}

	public int getBoardId() {
		return boardId;
	}

	public void setBoardId(int boardId) {
		this.boardId = boardId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "Article [boardId=" + boardId + ", memberId=" + memberId + ", title=" + title + ", body=" + body
				+ ", getId()=" + getId() + ", getRegDate()=" + getRegDate() + "]";
	}

}

class ArticleReply extends Dto {
	private int id;
	private String regDate;
	private int articleId;
	private int memberId;
	private String body;

	ArticleReply() {

	}

	public int getArticleId() {
		return articleId;
	}

	public void setArticleId(int articleId) {
		this.articleId = articleId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}

class Member extends Dto {
	private String loginId;
	private String loginPw;
	private String name;

	public Member() {

	}

	public Member(String loginId, String loginPw, String name) {
		this.loginId = loginId;
		this.loginPw = loginPw;
		this.name = name;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginPw() {
		return loginPw;
	}

	public void setLoginPw(String loginPw) {
		this.loginPw = loginPw;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

class Stat {
	private int totalMember;
	private int totalArticleNumber;
	private int totalArticleView;
	private int numberOfArticleInEachBoard;
	private int aritcleViewInEachBoard;
	ArticleDao articleDao;

//	void start() {
//		
//		getTotalMember();
//		System.out.println(getTotalMember());
//		String filePath = "db/article/index.json";
//		Util.writeJsonFile(filePath, stat);
//	}

	int totalMember() {
		int totalMember = Factory.getDB().getMembers().size();
		return totalMember;

	}

	int totalArticleNumber() {
		List<Article> articles = Factory.getArticleService().getArticles();
		int totalArticleNumber = articles.size();
		return totalArticleNumber;

	}

	int totalArticleView() {

		List<Article> articles = Factory.getArticleService().getArticles();
		for (Article article : articles) {
			totalArticleNumber += article.getViews();

		}
		return totalArticleNumber;
	}

	String numberOfArticleInEachBoard(Board board) {
		List<Board> boards = Factory.getArticleService().getBoards();

		for (Board board1 : boards) {
			List<Article> articles = Factory.getArticleService().getArticlesByBoardCode(board.getCode());
			numberOfArticleInEachBoard = articles.size();

			return board.getName() + " 게시물 수 : " + numberOfArticleInEachBoard;
		}
		return null;
	}

	String aritcleViewInEachBoard() {
		List<Board> boards = Factory.getArticleService().getBoards();

		for (Board board : boards) {
			List<Article> articles = Factory.getArticleService().getArticlesByBoardCode(board.getCode());

			int total = 0;
			for (Article article : articles) {
				total += article.getViews();

			}

			return board.getName() + " 조회수: " + total;
		}
		return null;
	}

	public int getTotalMember() {
		return totalMember;
	}

	public void setTotalMember(int totalMember) {
		this.totalMember = totalMember;
	}

	public int getTotalArticleNumber() {
		return totalArticleNumber;
	}

	public void setTotalArticleNumber(int totalArticleNumber) {
		this.totalArticleNumber = totalArticleNumber;
	}

	public int getTotalArticleView() {
		return totalArticleView;
	}

	public void setTotalArticleView(int totalArticleView) {
		this.totalArticleView = totalArticleView;
	}

	public int getNumberOfArticleInEachBoard() {
		return numberOfArticleInEachBoard;
	}

	public void setNumberOfArticleInEachBoard(int numberOfArticleInEachBoard) {
		this.numberOfArticleInEachBoard = numberOfArticleInEachBoard;
	}

	public int getAritcleViewInEachBoard() {
		return aritcleViewInEachBoard;
	}

	public void setAritcleViewInEachBoard(int aritcleViewInEachBoard) {
		this.aritcleViewInEachBoard = aritcleViewInEachBoard;
	}

}

// Util
class Util {
	// 현재날짜문장
	public static String getNowDateStr() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat Date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = Date.format(cal.getTime());
		return dateStr;
	}

	// 파일에 내용쓰기
	public static void writeFileContents(String filePath, int data) {
		writeFileContents(filePath, data + "");
	}

	// 첫 문자 소문자화
	public static String lcfirst(String str) {
		String newStr = "";
		newStr += str.charAt(0);
		newStr = newStr.toLowerCase();

		return newStr + str.substring(1);
	}

	// 파일이 존재하는지
	public static boolean isFileExists(String filePath) {
		File f = new File(filePath);
		if (f.isFile()) {
			return true;
		}

		return false;
	}

	// 파일내용 읽어오기
	public static String getFileContents(String filePath) {
		String rs = null;
		try {
			// 바이트 단위로 파일읽기
			FileInputStream fileStream = null; // 파일 스트림

			fileStream = new FileInputStream(filePath);// 파일 스트림 생성
			// 버퍼 선언
			byte[] readBuffer = new byte[fileStream.available()];
			while (fileStream.read(readBuffer) != -1) {
			}

			rs = new String(readBuffer);

			fileStream.close(); // 스트림 닫기
		} catch (Exception e) {
			e.getStackTrace();
		}

		return rs;
	}

	// 파일 쓰기
	public static void writeFileContents(String filePath, String contents) {
		BufferedOutputStream bs = null;
		try {
			bs = new BufferedOutputStream(new FileOutputStream(filePath));
			bs.write(contents.getBytes()); // Byte형으로만 넣을 수 있음
		} catch (Exception e) {
			e.getStackTrace();
		} finally {
			try {
				bs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Json안에 있는 내용을 가져오기
	public static Object getObjectFromJson(String filePath, Class cls) {
		ObjectMapper om = new ObjectMapper();
		Object obj = null;
		try {
			obj = om.readValue(new File(filePath), cls);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}

		return obj;
	}

	public static void writeJsonFile(String filePath, Object obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			om.writeValue(new File(filePath), obj);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void makeDir(String dirPath) {
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
}
