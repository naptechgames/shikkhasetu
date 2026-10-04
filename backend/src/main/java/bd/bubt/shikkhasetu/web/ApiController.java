package bd.bubt.shikkhasetu.web;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bd.bubt.shikkhasetu.allocation.AllocationPolicy;
import bd.bubt.shikkhasetu.model.AuthToken;
import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.notify.NotificationService;
import bd.bubt.shikkhasetu.service.AuthService;
import bd.bubt.shikkhasetu.service.DashboardService;
import bd.bubt.shikkhasetu.service.ItemService;
import bd.bubt.shikkhasetu.service.RequestService;
import bd.bubt.shikkhasetu.web.Dtos.HandoverBody;
import bd.bubt.shikkhasetu.web.Dtos.ItemBody;
import bd.bubt.shikkhasetu.web.Dtos.ItemDto;
import bd.bubt.shikkhasetu.web.Dtos.LoginBody;
import bd.bubt.shikkhasetu.web.Dtos.LoginDto;
import bd.bubt.shikkhasetu.web.Dtos.NewRequestBody;
import bd.bubt.shikkhasetu.web.Dtos.NotificationDto;
import bd.bubt.shikkhasetu.web.Dtos.RegisterBody;
import bd.bubt.shikkhasetu.web.Dtos.RequestDto;
import bd.bubt.shikkhasetu.web.Dtos.ReturnBody;
import bd.bubt.shikkhasetu.web.Dtos.ReviewBody;
import bd.bubt.shikkhasetu.web.Dtos.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * The REST API used by the mobile app. Controllers only translate between
 * JSON and the services; all rules and permission checks are in the services.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthService authService;
    private final ItemService itemService;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final DashboardService dashboardService;
    private final Clock clock;

    public ApiController(AuthService authService, ItemService itemService, RequestService requestService,
            NotificationService notificationService, DashboardService dashboardService, Clock clock) {
        this.authService = authService;
        this.itemService = itemService;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.dashboardService = dashboardService;
        this.clock = clock;
    }

    // ---------- health and authentication ----------

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "app", "ShikkhaSetu");
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterBody body) {
        return UserDto.of(authService.registerStudent(body.name(), body.email(), body.password()));
    }

    @PostMapping("/auth/login")
    public LoginDto login(@Valid @RequestBody LoginBody body) {
        AuthToken token = authService.login(body.email(), body.password());
        return new LoginDto(token.getToken(), UserDto.of(token.getUser()));
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        authService.logout(AuthInterceptor.tokenOf(request));
        return Map.of("status", "logged out");
    }

    @GetMapping("/me")
    public UserDto me(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return UserDto.of(user);
    }

    // ---------- items ----------

    @GetMapping("/items")
    public List<ItemDto> searchItems(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) ItemMode mode,
            @RequestParam(required = false) ItemStatus status) {
        return itemService.search(user, q, category, mode, status).stream().map(ItemDto::of).toList();
    }

    @GetMapping("/items/mine")
    public List<ItemDto> myItems(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return itemService.listOwnedBy(user).stream().map(ItemDto::of).toList();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto createItem(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @Valid @RequestBody ItemBody body) {
        return ItemDto.of(itemService.create(user, body.title(), body.description(), body.category(),
                body.mode(), body.condition()));
    }

    @PostMapping("/items/{id}/review")
    public ItemDto reviewItem(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id, @Valid @RequestBody ReviewBody body) {
        return ItemDto.of(itemService.review(user, id, body.approve()));
    }

    /** Strategy "first come, first served": approve the oldest pending request of this item. */
    @PostMapping("/items/{id}/allocate-fcfs")
    public RequestDto allocateFirstCome(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id) {
        return dto(requestService.allocate(user, id, new AllocationPolicy.FirstComeFirstServed(), null), user);
    }

    // ---------- requests ----------

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto createRequest(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @Valid @RequestBody NewRequestBody body) {
        return dto(requestService.create(user, body.itemId(), body.loanDays(), body.note()), user);
    }

    @GetMapping("/requests/mine")
    public List<RequestDto> myRequests(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return requestService.listMine(user).stream().map(r -> dto(r, user)).toList();
    }

    @GetMapping("/requests")
    public List<RequestDto> allRequests(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @RequestParam(required = false) RequestStatus status) {
        return requestService.listAll(user, status).stream().map(r -> dto(r, user)).toList();
    }

    @GetMapping("/requests/{id}")
    public RequestDto getRequest(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id) {
        return dto(requestService.get(user, id), user);
    }

    /** Strategy "coordinator choice": approve exactly this request. */
    @PostMapping("/requests/{id}/approve")
    public RequestDto approve(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id) {
        return dto(requestService.approve(user, id), user);
    }

    @PostMapping("/requests/{id}/reject")
    public RequestDto reject(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id) {
        return dto(requestService.reject(user, id), user);
    }

    @PostMapping("/requests/{id}/cancel")
    public RequestDto cancel(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id) {
        return dto(requestService.cancel(user, id), user);
    }

    @PostMapping("/requests/{id}/handover")
    public RequestDto handOver(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id, @Valid @RequestBody HandoverBody body) {
        return dto(requestService.handOver(user, id, body.pickupCode()), user);
    }

    @PostMapping("/requests/{id}/return")
    public RequestDto returnItem(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id, @Valid @RequestBody ReturnBody body) {
        return dto(requestService.returnItem(user, id, body.condition()), user);
    }

    // ---------- notifications and dashboard ----------

    @GetMapping("/notifications")
    public List<NotificationDto> notifications(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return notificationService.listFor(user).stream().map(NotificationDto::of).toList();
    }

    @PostMapping("/notifications/{id}/read")
    public NotificationDto markRead(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
            @PathVariable Long id) {
        return NotificationDto.of(notificationService.markRead(user, id));
    }

    @GetMapping("/dashboard")
    public DashboardService.Dashboard dashboard() {
        return dashboardService.build();
    }

    private RequestDto dto(ResourceRequest request, User viewer) {
        return RequestDto.of(request, viewer, LocalDate.now(clock));
    }
}
