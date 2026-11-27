"use client";

import {
  Children,
  createContext,
  isValidElement,
  useContext,
  useEffect,
  useMemo,
  useSyncExternalStore,
  type ReactElement,
  type ReactNode,
} from "react";

type NavigateOptions = { replace?: boolean };
type NavigateFunction = (to: string, options?: NavigateOptions) => void;
type RouterContextValue = { pathname: string; navigate: NavigateFunction };

const RouterContext = createContext<RouterContextValue | null>(null);
const routeSubscribers = new Set<() => void>();

function normalizedPathname() {
  if (typeof window === "undefined") return "/";
  const pathname = window.location.pathname.replace(/\/+$/, "");
  return pathname || "/";
}

export function BrowserRouter({ children }: { children: ReactNode }) {
  const pathname = useSyncExternalStore(
    (subscriber) => {
      routeSubscribers.add(subscriber);
      window.addEventListener("popstate", subscriber);
      return () => {
        routeSubscribers.delete(subscriber);
        window.removeEventListener("popstate", subscriber);
      };
    },
    normalizedPathname,
    () => "/",
  );

  const value = useMemo<RouterContextValue>(() => ({
    pathname,
    navigate: (to, options) => {
      const current = normalizedPathname();
      if (current === to) return;
      window.history[options?.replace ? "replaceState" : "pushState"]({}, "", to);
      routeSubscribers.forEach((subscriber) => subscriber());
      window.scrollTo({ top: 0, behavior: "auto" });
    },
  }), [pathname]);

  return <RouterContext.Provider value={value}>{children}</RouterContext.Provider>;
}

function useRouter() {
  const value = useContext(RouterContext);
  if (!value) throw new Error("Router hooks must be used inside BrowserRouter.");
  return value;
}

export function useLocation() {
  const { pathname } = useRouter();
  return { pathname };
}

export function useNavigate() {
  return useRouter().navigate;
}

export function Navigate({ to, replace = false }: { to: string; replace?: boolean }) {
  const { pathname, navigate } = useRouter();
  useEffect(() => {
    // During hydration the server snapshot is "/". Only redirect after the
    // browser snapshot agrees, otherwise a valid deep link could be replaced.
    if (normalizedPathname() === pathname) navigate(to, { replace });
  }, [navigate, pathname, replace, to]);
  return null;
}

type RouteProps = { path: string; element: ReactNode };

export function Route(props: RouteProps) {
  void props;
  return null;
}

export function Routes({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const routes = Children.toArray(children).filter(isValidElement) as ReactElement<RouteProps>[];
  const match = routes.find((route) => route.props.path === pathname)
    ?? routes.find((route) => route.props.path === "*");
  return match?.props.element ?? null;
}
