import { Button, Typography } from "@material-tailwind/react";
import { ScrollRestoration, useLoaderData, useNavigate } from "react-router-dom";
import UseAuth from "../../Hook/UseAuth";
import toast from "react-hot-toast";
import axios from "axios";
import { Helmet } from "react-helmet";
import PropTypes from "prop-types";
import { useState, useEffect } from 'react';


const PostDetails = ({ title }) => {
  const post = useLoaderData();
  const { user } = UseAuth();
  const navigate = useNavigate();
  const {
    id,
    postTitle,
    category,
    location,
    thumbnail,
    noOfVolunteer,
    deadline,
    description,
    orgEmail,
    orgName
  } = post;
  const [alreadyRegistered, setAlreadyRegistered] = useState(false);
  const [checkingRegistration, setCheckingRegistration] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const check = async () => {
      if (!user || !user.email) return setAlreadyRegistered(false);
      setCheckingRegistration(true);
      try {
        const email = user.email;
        const url = `${import.meta.env.VITE_API_URL}/get-volunteer-request/${encodeURIComponent(email)}?postId=${id}&size=1`;
        const res = await axios.get(url, { withCredentials: true });
        let content = [];
        if (Array.isArray(res.data)) {
          content = res.data;
        } else if (res.data && Array.isArray(res.data.content)) {
          content = res.data.content;
        }
        const already = content.some((r) => r.postId === id);
        if (!cancelled) setAlreadyRegistered(Boolean(already));
      } catch (err) {
        console.error('Failed to check existing registration', err);
        if (!cancelled) setAlreadyRegistered(false);
      } finally {
        if (!cancelled) setCheckingRegistration(false);
      }
    };
    check();
    return () => { cancelled = true; };
  }, [user, id]);

  const handleVolunteer = async () => {
    console.log("I want to be a volunteer !");

    if (noOfVolunteer <= 0) {
      toast.error("The maximum number of volunteers is already filled!");
      return;
    }
    if (!user) {
      // Let the PrivateRoutes at target handle redirect, but give user a hint
      toast.error("Please log in to register as a volunteer.");
      return navigate(`/login`);
    }
    if (user?.email === orgEmail) {
      return toast.error("You can't be a volunteer for your own post!");
    }

    // Defensive check in case state is stale
    if (alreadyRegistered) {
      return toast.error("You have already registered for this post.");
    }

    navigate(`/be-a-volunteer/${id}`);
  };
  return (
    <div>
      <ScrollRestoration></ScrollRestoration>
      <Helmet>
        <title>
          {title}
        </title>
      </Helmet>
      <section className="py-16 font-qs px-8 ">
        <div data-aos="fade-up" data-aos-easing="linear" data-aos-duration="1500" className="mx-auto container grid place-items-center gap-12 grid-cols-1 md:grid-cols-2">
          <img
            src={thumbnail}
            alt="pink blazer"
            className="h-[36rem] rounded-md object-cover"
          />
          <div className="space-y-6">
            <Typography className="mb-4" variant="h2">
              {postTitle}
            </Typography>
            <Typography variant="h4">
              Category :{" "}
              <span className="text-green-500 font-qs font-bold">
                {category}
              </span>
            </Typography>
            <Typography className="!mt-4 text-base font-normal leading-[27px] !text-gray-500">
              {description}
            </Typography>
            <div className="my-4 flex items-center gap-2">
              <Typography className="text-xl font-semibold  ">
                Location :{" "}
                <span className="font-bold font-qs bg-yellow-300 px-4 py-2 rounded-md">
                  {location}
                </span>
              </Typography>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2">
              <div className=" flex items-center gap-2">
                <Typography

                  className="text-lg font-semibold "
                >
                  Deadline :
                </Typography>
                <Typography

                  className="text-lg font-qs font-bold "
                >
                  {deadline}
                </Typography>
              </div>
              <div className=" flex items-center gap-2">
                <Typography

                  className="text-lg font-semibold "
                >
                  Number of Volunteer Need :
                </Typography>
                <Typography
                  color="blue-gray"
                  className="text-lg font-qs font-bold bg-green-300 px-3 py-2 rounded-full "
                >
                  {noOfVolunteer}
                </Typography>
              </div>
            </div>
            <div className="my-4 pb-8 ">
              <Typography

                className="text-lg font-qs font-bold pb-4"
              >
                Organization Information :
              </Typography>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className=" flex items-center gap-2">
                  <Typography

                    className="text-lg font-qs font-bold"
                  >
                    Name :
                  </Typography>
                  <Typography

                    className="text-lg font-qs font-bold"
                  >
                    {orgName}
                  </Typography>
                </div>
                <div className=" flex flex-col md:flex-row  md:items-center md:gap-2">
                  <Typography

                    className="text-lg font-qs font-bold"
                  >
                    Email:
                  </Typography>
                  <Typography

                    className="text-lg font-qs font-bold"
                  >{orgEmail}
                  </Typography>
                </div>
              </div>
            </div>
            <div className="mb-4 flex w-full items-center gap-3 md:w-1/2 ">
              {alreadyRegistered ? (
                <Button disabled color="gray" className="w-52">You Have Registered</Button>
              ) : (
                <Button
                  onClick={handleVolunteer}
                  color="red"
                  variant="gradient"
                  className="w-52"
                >
                  Be A Volunteer
                </Button>
              )}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
PostDetails.propTypes = {
  title: PropTypes.object.isRequired,
}
export default PostDetails;
