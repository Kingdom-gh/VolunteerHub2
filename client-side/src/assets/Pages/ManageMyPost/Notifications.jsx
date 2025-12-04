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
        setNotifications(data.content || data);
        setTotal(data.totalElements || 0);
      } catch (err) {
        console.error(err);
      }
    };
    fetchNotifications();
  }, [user, page]);

  return (
    <div className="p-6">
      <h2 className="text-2xl font-bold mb-4">Notifications</h2>
      {notifications.length === 0 ? (
        <div>No notifications</div>
      ) : (
        <div className="space-y-2">
          {notifications.map((n) => (
            <div key={n.id} className="border p-3 rounded-md">
              <div className="font-semibold">{n.title}</div>
              <div className="text-sm">{n.body}</div>
              <div className="text-xs text-gray-500">{new Date(n.createdAt).toLocaleString()}</div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-4 flex items-center gap-3">
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
  );
};

export default Notifications;
