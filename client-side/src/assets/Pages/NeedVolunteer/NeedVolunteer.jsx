import { Link, useNavigation } from "react-router-dom";
import VolunteerNeedsCard from "../Homepage/VolunteerNeeds/VolunteerNeedsCard";
import { useState } from "react";
import * as React from "react";
import ViewListIcon from "@mui/icons-material/ViewList";
import ViewModuleIcon from "@mui/icons-material/ViewModule";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import { Button } from "@material-tailwind/react";
import { useEffect } from "react";
import axios from "axios";
import { Helmet } from "react-helmet";
import PropTypes from "prop-types";
import LoadingGif from "../../Components/Loader/LoadingGif";

const NeedVolunteer = ({ title }) => {
  const [volunteers, setVolunteers] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState("");
  const [searchText, setSearchText] = useState("");
  const [showLoader, setShowLoader] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setShowLoader(false);
    }, 1000);

    return () => clearTimeout(timer);
  }, []);
  useEffect(() => {
    const getData = async () => {
      try {
        const { data } = await axios(
          `${import.meta.env.VITE_API_URL}/need-volunteers?search=${encodeURIComponent(search)}&page=${page}&size=${size}`
        );

        let content = [];
        let tp = 0;
        if (Array.isArray(data)) {
          content = data;
          tp = 1;
        } else if (data && Array.isArray(data.content)) {
          content = data.content;
          tp = typeof data.totalPages === "number" ? data.totalPages : 1;
        } else {

          content = data || [];
          tp = data && data.totalPages ? data.totalPages : 0;
        }
        setVolunteers(content);
        setTotalPages(tp || 0);
      } catch (e) {
        setVolunteers([]);
        setTotalPages(0);
      }
    };
    getData();
  }, [search, page, size]);
  console.log(volunteers);
  const [view, setView] = React.useState("module");

  const [gridView, setGridView] = useState(true);
  const [tableView, setTableView] = useState(false);

  const handleChange = (event, nextView) => {
    setView(nextView);
  };

  const handleSearch = () => {
    setPage(0);
    setSearch(searchText.trim());
  };

  const handleNextPage = () => {
    if (page + 1 < totalPages) setPage(page + 1);
  };
  const handlePrevPage = () => {
    if (page > 0) setPage(page - 1);
  };
  const handleSizeChange = (e) => {
    const newSize = parseInt(e.target.value, 10) || 20;
    setSize(newSize > 100 ? 100 : newSize);
    setPage(0);
  };

  const handleGrid = (e) => {
    setTableView(!e);
    setGridView(e);
  };
  const handleTable = (e) => {
    setTableView(e);
    setGridView(!e);
    console.log(tableView);
  };
  const navigation = useNavigation();
  if (navigation.state === "loading") return <LoadingGif />;
  return (
    <div className="py-16 font-qs">
      <Helmet>
        <title>{title}</title>
      </Helmet>
      <div
        data-aos="fade-left"
        data-aos-anchor-placement="top-bottom"
        data-aos-easing="linear"
        data-aos-duration="1500"
        className="container mx-auto mb-6"
      >
        <h2 className="text-2xl md:text-5xl font-bold text-center ">
          Join Our Community: Volunteer Opportunities
        </h2>
        <p className="w-2/3 mx-auto md:text-lg mt-4 text-center leading-relaxed ">
          Discover meaningful ways to contribute to our mission. Explore diverse
          volunteer roles and make a difference in your community. Join us in
          creating positive change today.
        </p>
      </div>
      <div className="flex flex-col md:flex-row items-center justify-between container mx-auto">
        <div
          data-aos="fade-down"
          data-aos-anchor-placement="top-bottom"
          data-aos-easing="linear"
          data-aos-duration="1000"
          className="flex container mx-auto justify-center my-8 md:justify-start "
        >
          <ToggleButtonGroup
            orientation="horizontal"
            value={view}
            exclusive
            className="bg-gray-200"
            onChange={handleChange}
          >
            <ToggleButton
              onClick={() => handleGrid(true)}
              value="module"
              aria-label="module"
            >
              <ViewModuleIcon />
            </ToggleButton>
            <ToggleButton
              onClick={() => handleTable(true)}
              value="list"
              aria-label="list"
            >
              <ViewListIcon />
            </ToggleButton>
          </ToggleButtonGroup>
        </div>
        <div
          data-aos="fade-down"
          data-aos-anchor-placement="top-bottom"
          data-aos-easing="linear"
          data-aos-duration="1000"
          className="flex container  mx-auto justify-center my-8 md:justify-end"
        >
          <div className="flex p-1  border rounded-lg    focus-within:ring focus-within:ring-opacity-40 focus-within:border-blue-400 focus-within:ring-blue-300">
            <input
              className="px-6 py-2 border-none text-gray-700 placeholder-gray-500 bg-white outline-none focus:placeholder-transparent"
              type="text"
              onChange={(e) => setSearchText(e.target.value)}
              value={searchText}
              name="search"
              placeholder="Enter the Post Title"
              aria-label="Enter the Post Title"
            />

            <button
              onClick={() => handleSearch()}
              type="button"
              className="inline-block rounded-lg bg-warning px-6 pb-2 pt-2.5 text-xs font-medium uppercase leading-normal text-white shadow-warning-3 transition duration-150 ease-in-out hover:bg-warning-accent-300 hover:shadow-warning-2 focus:bg-warning-accent-300 focus:shadow-warning-2 focus:outline-none focus:ring-0 active:bg-warning-600 active:shadow-warning-2 motion-reduce:transition-none dark:shadow-black/30 dark:hover:shadow-dark-strong dark:focus:shadow-dark-strong dark:active:shadow-dark-strong"
            >
              Search
            </button>
          </div>
        </div>
      </div>
      {volunteers.length === 0 && showLoader ? (
        <LoadingGif></LoadingGif>
      ) : (
        <div>
          {/* Pagination Controls */}
          <div className="container mx-auto flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={handlePrevPage}
                className="px-3 py-1 rounded bg-gray-200 disabled:opacity-50"
              >
                Prev
              </button>
              <button
                disabled={page + 1 >= totalPages}
                onClick={handleNextPage}
                className="px-3 py-1 rounded bg-gray-200 disabled:opacity-50"
              >
                Next
              </button>
              <span className="text-sm">
                Page {page + 1} / {totalPages || 1}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm">Size:</label>
              <select
                value={size}
                onChange={handleSizeChange}
                className="border rounded px-2 py-1"
              >
                {[10, 20, 30, 50, 100].map(s => <option key={s} value={s}>{s}</option>)}
              </select>
              <span className="text-xs text-gray-500">(max 100)</span>
            </div>
          </div>
          <div className={gridView ? "block" : "hidden"}>
            <div className=" container mx-auto mt-16 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-y-6 md:gap-y-12">
              {volunteers.map((volunteer) => (
                <VolunteerNeedsCard
                  volunteer={volunteer}
                  key={volunteer.id}
                ></VolunteerNeedsCard>
              ))}
            </div>
          </div>
          <div className={!tableView ? "hidden" : "block"}>
            <div data-aos="fade-up" data-aos-easing="linear" data-aos-duration="1500" className="container mx-auto mt-16">
              <div className="hidden md:block">
                <div className="overflow-x-auto ">
                  <table className="table border-collapse border border-gray-400">
                    {/* head */}
                    <thead>
                      <tr className="text-white raleway text-base bg-[#DE00DF]">
                        <th></th>
                        <th>Post Title</th>
                        <th>Posted By</th>
                        <th>Category </th>
                        <th>Deadline </th>
                        <th>Location</th>
                        <th>Volunteer Needed</th>
                        <th>View Details</th>
                      </tr>
                    </thead>
                    <tbody>
                      {/* row 1 */}
                      {volunteers.map((post, idx) => (
                        <tr className="border border-gray-300" key={post.id}>
                          <th className="font-semibold">{idx + 1}</th>
                          <td className="font-semibold">{post.postTitle}</td>
                          <td className="font-semibold">
                            {post.orgName}
                          </td>
                          <td className="font-semibold">{post.category}</td>
                          <td className="font-semibold">{post.deadline}</td>
                          <td className="font-semibold">{post.location}</td>
                          <td className="font-semibold text-center">
                            {post.noOfVolunteer}
                          </td>
                          <td>
                            <Link to={`/post-details/${post.id}`}>
                              <Button color="green">View Details</Button>
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Small  */}
              <div>
                <div data-aos="fade-up" data-aos-easing="linear" data-aos-duration="1500" className=" md:hidden">
                  <div className="overflow-x-auto ">
                    <table className="table border-collapse border border-gray-400">
                      {/* head */}
                      <thead>
                        <tr className="text-white raleway text-base bg-[#DE00DF]">
                          <th>Post Title </th>
                          <th>Deadline</th>
                          <th>View Details</th>
                        </tr>
                      </thead>
                      <tbody>
                        {/* row 1 */}
                        {volunteers.map((post) => (
                          <tr className="border border-gray-300" key={post.id}>
                            <td>{post.postTitle}</td>
                            <td>{post.deadline}</td>
                            <td>
                              <Link to={`/post-details/${post.id}`}>
                                <Button color="green">View Details</Button>
                              </Link>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
NeedVolunteer.propTypes = {
  title: PropTypes.object.isRequired,
};
export default NeedVolunteer;
