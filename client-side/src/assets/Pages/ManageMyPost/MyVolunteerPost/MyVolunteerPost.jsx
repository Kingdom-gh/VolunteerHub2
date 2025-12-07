import axios from "axios";
import { useEffect, useState } from "react";
import { Link, useNavigate, useNavigation } from "react-router-dom";
import { MdEdit, MdDelete } from "react-icons/md";
import UseAuth from "../../../Hook/UseAuth";
import Swal from "sweetalert2";
import PageError from "../../ErrorPage/PageError";
import PropTypes from "prop-types";
import Loader from "../../../Components/Loader/Loader";
import LoadingGif from "../../../Components/Loader/LoadingGif";
import { Helmet } from "react-helmet";

const MyVolunteerPost = ({ title, onOpenRequests }) => {
  const { user } = UseAuth();
  const navigate = useNavigate();
  const [showLoader, setShowLoader] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setShowLoader(false);
    }, 1000);

    return () => clearTimeout(timer);
  }, []);
  const [myVolunteerPost, setMyVolunteerPost] = useState([]);
  const [pendingCounts, setPendingCounts] = useState({});
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const PAGE_SIZE = 10;
  console.log(myVolunteerPost);
  useEffect(() => { setPage(0); }, [user?.email]);

  const fetchPendingCounts = async (postsForPage) => {
    if (!user?.email) {
      setPendingCounts({});
      return;
    }
    const ids = (postsForPage || []).map((item) => item?.id).filter(Boolean);
    if (ids.length === 0) {
      setPendingCounts({});
      return;
    }
    const params = ids.map((id) => `postIds=${id}`).join("&");
    const { data } = await axios(
      `${import.meta.env.VITE_API_URL}/org/${user.email}/posts-with-pending-count?${params}`,
      { withCredentials: true }
    );
    const map = {};
    (data || []).forEach((item) => { map[item.id] = item.pendingCount ?? 0; });
    setPendingCounts(map);
  };

  useEffect(() => {
    const volunteers = async () => {
      if (!user?.email) return;
      const url = `${import.meta.env.VITE_API_URL}/get-volunteer-post/${user.email}?page=${page}&size=${PAGE_SIZE}`;
      const { data } = await axios(url, { withCredentials: true });
      let content = [];
      let tp = 0;
      let total = 0;
      if (Array.isArray(data)) {
        content = data;
        tp = 1;
        total = data.length;
      } else if (data && Array.isArray(data.content)) {
        content = data.content;
        tp = typeof data.totalPages === "number" ? data.totalPages : 1;
        total = typeof data.totalElements === "number" ? data.totalElements : content.length;
      }
      setMyVolunteerPost(content);
      setTotalPages(tp);
      setTotalElements(total);
      await fetchPendingCounts(content);
    };
    volunteers();
  }, [user?.email, page]);

  const handlePrevPage = () => {
    if (page > 0) {
      setPage((p) => p - 1);
    }
  };

  const handleNextPage = () => {
    if (page + 1 < totalPages) {
      setPage((p) => p + 1);
    }
  };

  const handleDelete = (id) => {
    Swal.fire({
      title: "Are you sure?",
      text: "You won't be able to revert this!",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Yes, delete it!",
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${import.meta.env.VITE_API_URL}/my-volunteer-post/${id}`, {
          method: "DELETE",
        })
          .then((res) => res.json())
          .then((data) => {
            if (data.deletedCount > 0) {
              Swal.fire({
                title: "Deleted!",
                text: "Your post has been deleted.",
                icon: "success",
              });
            }
            const remaining = myVolunteerPost.filter((post) => post.id !== id);
            setMyVolunteerPost(remaining);
            fetchPendingCounts(remaining);
            navigate(`/manage-my-post`);
          });
      }
    });
  };

  // Note: approve/reject moved to RequestDetails tab; this view only shows pending counts

  // Loading:
  const navigation = useNavigation();
  if (navigation.state === "loading") return <Loader />;

  return (
    <div className="container font-qs mx-auto space-y-5">
      <Helmet>
        <title>
          {title}
        </title>
      </Helmet>
      {myVolunteerPost.length > 0 ? (
        <div>
          <h2 className="text-5xl font-bold my-6 text-center mt-6">
            Total Posts: {totalElements}
          </h2>
          <div className="flex items-center justify-center gap-4 mb-4">
            <button disabled={page === 0} onClick={handlePrevPage} className="btn btn-sm">
              Prev
            </button>
            <span>Page {page + 1} / {totalPages || 1}</span>
            <button disabled={page + 1 >= totalPages} onClick={handleNextPage} className="btn btn-sm">
              Next
            </button>
          </div>
          <div className="hidden md:block">
            <div className="overflow-x-auto ">
              <table className="table border-collapse border border-gray-400">
                {/* head */}
                <thead>
                  <tr className="text-white raleway text-base bg-[#DE00DF]">
                    <th></th>
                    <th>Post Title</th>
                    <th>Category </th>
                    <th>Deadline </th>
                    <th>Location</th>
                    <th>Actions</th>
                    <th>Requests</th>
                  </tr>
                </thead>
                <tbody>
                  {/* row 1 */}
                  {myVolunteerPost.map((post, idx) => (
                    <tr className="border border-gray-300" key={post.id}>
                      <th className="font-semibold">{page * PAGE_SIZE + idx + 1}</th>
                      <td className="font-semibold">{post.postTitle}</td>
                      <td className="font-semibold">{post.category}</td>
                      <td className="font-semibold">{post.deadline}</td>
                      <td className="font-semibold">{post.location}</td>

                      <td>
                        <div className="flex items-center gap-6">
                          <Link to={`/update-my-post/${post.id}`}>
                            <MdEdit className="size-6" />
                          </Link>
                          <button onClick={() => handleDelete(post.id)}>
                            <MdDelete className="size-6" />
                          </button>
                        </div>
                      </td>
                      <td>
                        <div>
                          <span onClick={() => onOpenRequests?.(post.id)} role="button" tabIndex={0} className="badge badge-sm bg-yellow-500 text-white cursor-pointer">Pending: {pendingCounts[post.id] ?? 0}</span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div>
            <div className=" md:hidden">
              <div className="overflow-x-auto ">
                <table className="table border-collapse border border-gray-400">
                  {/* head */}
                  <thead>
                    <tr className="text-white raleway text-base bg-[#DE00DF]">
                      <th>Post Title </th>
                      <th>Category</th>
                      <th>Actions</th>
                      <th>Requests</th>
                    </tr>
                  </thead>
                  <tbody>
                    {/* row 1 */}
                    {myVolunteerPost.map((post) => (
                      <tr className="border border-gray-300" key={post.id}>
                        <td>{post.postTitle}</td>
                        <td>{post.category}</td>
                        <td>
                          <div className="flex items-center gap-6">
                            <Link to={`/update-my-post/${post.id}`}>
                              <MdEdit className="size-6" />
                            </Link>
                            <button onClick={() => handleDelete(post.id)}>
                              <MdDelete className="size-6" />
                            </button>
                          </div>
                        </td>
                        <td>
                          <div>
                            <span onClick={() => onOpenRequests?.(post.id)} role="button" tabIndex={0} className="badge badge-sm bg-yellow-500 text-white cursor-pointer">Pending: {pendingCounts[post.id] ?? 0}</span>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      ) : showLoader ? (
        <LoadingGif></LoadingGif>
      ) : (
        <PageError></PageError>
      )}
    </div>
  );
};
MyVolunteerPost.propTypes = {
  title: PropTypes.object.isRequired,
  onOpenRequests: PropTypes.func,
};
export default MyVolunteerPost;
