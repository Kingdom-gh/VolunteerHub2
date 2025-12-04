import { useEffect, useState } from "react";
import axios from "axios";
import { MdCheckCircle, MdCancel } from "react-icons/md";
import Swal from "sweetalert2";
import UseAuth from "../../../Hook/UseAuth";

const RequestDetails = ({ selectedPostId: selectedPostIdFromParent }) => {
    const { user } = UseAuth();
    const [posts, setPosts] = useState([]);
    const [counts, setCounts] = useState({});
    const [selectedPostId, setSelectedPostId] = useState(null);
    // allow parent to control selected post
    // `selectedPostIdFromParent` prop will override internal selection when provided
    // (prop name: selectedPostId)
    // if parent passes null/undefined, behave normally
    useEffect(() => {
        if (typeof selectedPostIdFromParent !== 'undefined' && selectedPostIdFromParent !== null) {
            setSelectedPostId(selectedPostIdFromParent);
        }
    }, [selectedPostIdFromParent]);

    const [page, setPage] = useState(0);
    const PAGE_SIZE = 10;
    const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });

    const loadPosts = async () => {
        if (!user?.email) return;
        const { data } = await axios(`${import.meta.env.VITE_API_URL}/get-volunteer-post/${user.email}`, { withCredentials: true });
        setPosts(data || []);
        // Only set default if parent didn't provide a selectedPostId
        if ((typeof selectedPostIdFromParent === 'undefined' || selectedPostIdFromParent === null) && data && data.length > 0) {
            setSelectedPostId(data[0].id);
        }
    };

    const loadCounts = async () => {
        if (!user?.email) return;
        const { data } = await axios(`${import.meta.env.VITE_API_URL}/org/${user.email}/posts-with-pending-count`, { withCredentials: true });
        const map = {};
        (data || []).forEach(item => { map[item.id] = item.pendingCount; });
        setCounts(map);
    };

    const loadPage = async (p = page, postId = selectedPostId) => {
        if (!postId) {
            setData({ content: [], totalPages: 0, totalElements: 0 });
            return;
        }
        const res = await axios(`${import.meta.env.VITE_API_URL}/post/${postId}/requests?page=${p}`, { withCredentials: true });
        const d = res.data || {};
        setData({ content: d.content || [], totalPages: d.totalPages || 0, totalElements: d.totalElements || 0 });
    };

    useEffect(() => { loadPosts(); loadCounts(); }, [user?.email]);
    useEffect(() => { setPage(0); loadPage(0); }, [selectedPostId]);

    const approve = async (id) => {
        try {
            await axios.put(`${import.meta.env.VITE_API_URL}/volunteer-request/${id}/approve`, {}, { withCredentials: true });
            Swal.fire({ title: "Approved", icon: "success" });
            await loadCounts();
            loadPage(page);
        } catch (err) { Swal.fire({ title: "Failed", text: err?.response?.data?.message || "Approval failed", icon: "error" }); }
    };
    const reject = async (id) => {
        try {
            await axios.put(`${import.meta.env.VITE_API_URL}/volunteer-request/${id}/reject`, {}, { withCredentials: true });
            Swal.fire({ title: "Rejected", icon: "success" });
            await loadCounts();
            loadPage(page);
        } catch (err) { Swal.fire({ title: "Failed", text: err?.response?.data?.message || "Rejection failed", icon: "error" }); }
    };

    if (!user) return <div className="p-4">Please login to manage your posts.</div>;

    return (
        <div className="container font-qs mx-auto space-y-5 p-4">
            <h3 className="text-xl font-semibold mb-4">Requests Details</h3>

            <div className="mb-4 flex items-center gap-4">
                <label className="font-medium">Select post:</label>
                <select value={selectedPostId ?? ""} onChange={(e) => setSelectedPostId(Number(e.target.value) || null)} className="border p-2">
                    {posts.length === 0 && <option value="">No posts found</option>}
                    {posts.map(p => (
                        <option key={p.id} value={p.id}>
                            {p.postTitle} {counts[p.id] ? `(${counts[p.id]} pending)` : ""}
                        </option>
                    ))}
                </select>
                <div className="ml-auto font-medium">Total requests: {data.totalElements}</div>
            </div>

            <div>
                <div className="hidden md:block overflow-x-auto">
                    <table className="table border-collapse border border-gray-400 w-full">
                        <thead>
                            <tr className="text-white raleway text-base bg-[#DE00DF]">
                                <th></th>
                                <th>Post Title</th>
                                <th>Category</th>
                                <th>Deadline</th>
                                <th>Location</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.content.map((r, i) => {
                                const idx = page * PAGE_SIZE + i + 1;
                                return (
                                    <tr className="border border-gray-300" key={r.id}>
                                        <th className="font-semibold">{idx}</th>
                                        <td className="font-semibold p-2">{r.postTitle}</td>
                                        <td className="font-semibold p-2">{r.category}</td>
                                        <td className="font-semibold p-2">{r.deadline}</td>
                                        <td className="font-semibold p-2">{r.location}</td>
                                        <td className="font-semibold p-2">{r.status || 'Pending'}</td>
                                        <td className="p-2">
                                            {(!r.status || String(r.status).toLowerCase() === "pending") ? (
                                                <div className="flex gap-3">
                                                    <MdCheckCircle title="Accept" onClick={() => approve(r.id)} className="size-5 cursor-pointer text-green-600" />
                                                    <MdCancel title="Reject" onClick={() => reject(r.id)} className="size-5 cursor-pointer text-red-600" />
                                                </div>
                                            ) : (
                                                <span className="text-sm text-gray-500">{r.status}</span>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                            {data.content.length === 0 && (
                                <tr><td colSpan={7} className="p-4 text-center text-gray-500">No requests for this post.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>

                {/* Mobile view: simplified rows similar to MyVolunteerPost small-screen table */}
                <div className="md:hidden">
                    <div className="overflow-x-auto">
                        <table className="table border-collapse border border-gray-400 w-full">
                            <thead>
                                <tr className="text-white raleway text-base bg-[#DE00DF]">
                                    <th>Post Title</th>
                                    <th>Category</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {data.content.map((r) => (
                                    <tr className="border border-gray-300" key={r.id}>
                                        <td className="p-2">{r.postTitle}</td>
                                        <td className="p-2">{r.category}</td>
                                        <td className="p-2">{r.status || 'Pending'}</td>
                                        <td className="p-2">
                                            {(!r.status || String(r.status).toLowerCase() === "pending") ? (
                                                <div className="flex gap-3">
                                                    <MdCheckCircle title="Accept" onClick={() => approve(r.id)} className="size-5 cursor-pointer text-green-600" />
                                                    <MdCancel title="Reject" onClick={() => reject(r.id)} className="size-5 cursor-pointer text-red-600" />
                                                </div>
                                            ) : (
                                                <span className="text-sm text-gray-500">{r.status}</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                {data.content.length === 0 && (
                                    <tr><td colSpan={4} className="p-4 text-center text-gray-500">No requests for this post.</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div className="flex items-center gap-4 mt-4">
                <button disabled={page <= 0} onClick={() => { setPage(page - 1); loadPage(page - 1); }} className="btn btn-sm">Prev</button>
                <span>Page {page + 1} / {data.totalPages}</span>
                <button disabled={page + 1 >= data.totalPages} onClick={() => { setPage(page + 1); loadPage(page + 1); }} className="btn btn-sm">Next</button>
            </div>
        </div>
    );
};

export default RequestDetails;