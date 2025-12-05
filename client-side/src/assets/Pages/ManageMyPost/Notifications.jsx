import { useEffect, useState } from "react";
import UseAuth from "../../Hook/UseAuth";
import axios from "axios";

const Notifications = () => {
  const { user } = UseAuth();
  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const pageSize = 10;

  useEffect(() => {
    if (!user) return;
    const fetchNotifications = async () => {
      try {
        const res = await axios.get(`${import.meta.env.VITE_API_URL}/api/notifications?page=${page}`, { withCredentials: true });
        const data = res.data;
        const list = data?.content || data || [];
        setNotifications(list);
        setTotal(data?.totalElements || 0);
      } catch (err) {
        console.error(err);
      }
    };
    fetchNotifications();
    // SSE removed: notifications are stored in DB only; realtime push disabled.
    return () => { };
  }, [user, page]);

  const colorForTitle = (title) => {
    if (!title) return "bg-white border-gray-200";
    const t = title.toLowerCase();
    if (t.includes("accepted") || t.includes("approved")) return "bg-green-50 border-green-300";
    if (t.includes("rejected")) return "bg-red-50 border-red-300";
    if (t.includes("already registered")) return "bg-black-50 border-black-300";
    if (t.includes("event removed") || t.includes("invalid")) return "bg-gray-50 border-gray-300";
    if (t.includes("request submitted")) return "bg-blue-50 border-blue-300";
    if (t.includes("new volunteer request")) return "bg-indigo-50 border-indigo-300";
    return "bg-white border-gray-200";
  };

  return (
    <div className="mt-16">
      <div className="mx-8 md:mx-0 flex items-center justify-center">
        <div className="w-full max-w-3xl p-6">
          <h2 className="text-2xl font-bold mb-4">Notifications</h2>

          {notifications.length === 0 ? (
            <div className="text-center text-gray-500 py-12">No notifications</div>
          ) : (
            <div className="space-y-3">
              {notifications.map((n) => {
                const classes = colorForTitle(n.title);
                return (
                  <div key={n.id} className={`${classes} border p-4 rounded-md`}>
                    <div>
                      <div className="font-semibold text-lg">{n.title}</div>
                      <div className="text-sm text-gray-700 mt-1">{n.body}</div>
                      <div className="text-xs text-gray-500 mt-2">{new Date(n.createdAt).toLocaleString()}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <div className="mt-6 flex items-center justify-center gap-3">
            <button
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="btn btn-sm"
            >
              Prev
            </button>
            <div>Page {page + 1} / {Math.max(1, Math.ceil(total / pageSize))}</div>
            <button
              disabled={(page + 1) * pageSize >= total}
              onClick={() => setPage((p) => p + 1)}
              className="btn btn-sm"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Notifications;
